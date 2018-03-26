/**
 * 
 */
package de.evoila.cf.broker.custom.mongodb;

import com.mongodb.BasicDBObject;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MongoDbBindingService extends BindingServiceImpl {

	private Logger log = LoggerFactory.getLogger(MongoDbBindingService.class);

    private static String URI = "uri";
	private static String USERNAME = "user";
    private static String PASSWORD = "password";
    private static String DATABASE = "database";

	private RandomString usernameRandomString = new RandomString(10);
    private RandomString passwordRandomString = new RandomString(15);

    @Autowired(required = false)
    private ExistingEndpointBean existingEndpointBean;

    @Override
    protected ServiceInstanceBinding bindService(String bindingId, ServiceInstance serviceInstance, Plan plan) {
        Map<String, Object> credentials = createCredentials(bindingId, serviceInstance, plan, null);

        return new ServiceInstanceBinding(bindingId, serviceInstance.getId(), credentials, null);
    }

    @Override
    protected void deleteBinding(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) {
        MongoDbService mongoDbService = connection(serviceInstance, plan);

        mongoDbService.mongoClient().getDatabase(binding.getCredentials().get(DATABASE).toString())
                .runCommand(new BasicDBObject("dropUser", binding.getCredentials().get(USERNAME)));
    }

    @Override
    public ServiceInstanceBinding getServiceInstanceBinding(String id) {
        throw new UnsupportedOperationException();
    }


	@Override
	protected ServiceInstanceBinding bindServiceKey(String bindingId, ServiceInstance serviceInstance, Plan plan,
			List<ServerAddress> externalAddresses) {

		Map<String, Object> credentials = createCredentials(bindingId, serviceInstance, plan, externalAddresses.get(0));

		ServiceInstanceBinding serviceInstanceBinding = new ServiceInstanceBinding(bindingId, serviceInstance.getId(),
				credentials, null);
		serviceInstanceBinding.setExternalServerAddresses(externalAddresses);
		return serviceInstanceBinding;
	}

	@Override
	protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
		throw new UnsupportedOperationException();
	}

    @Override
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress host) {

        MongoDbService mongoDbService = connection(serviceInstance, plan);

        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();
        String database = serviceInstance.getId();

        MongoDBCustomImplementation.createUserForDatabase(mongoDbService, database, username, password);

        String endpoint = ServiceInstanceUtils.connectionUrl(serviceInstance.getHosts());

        // When host is not empty, it is a service key
        if (host != null)
            endpoint = host.getIp() + ":" + host.getPort();

        String dbURL = String.format("mongodb://%s:%s@%s/%s", username, password, endpoint, database);
        String replicaSet = serviceInstance.getParameters().get("replicaSet");

        if (replicaSet != null && !replicaSet.equals(""))
            dbURL += String.format("?replicaSet=%s", replicaSet);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put(URI, dbURL);
        credentials.put(USERNAME, username);
        credentials.put(PASSWORD, password);
        credentials.put(DATABASE, database);

        return credentials;
    }

    private MongoDbService connection(ServiceInstance serviceInstance, Plan plan) {
        MongoDbService mongoDbService = new MongoDbService();
        ServerAddress host = serviceInstance.getHosts().get(0);
        log.info("Opening connection to " + host.getIp() + ":" + host.getPort());

        if(plan.getPlatform() == Platform.BOSH)
            mongoDbService.createConnection(serviceInstance.getUsername(), serviceInstance.getPassword(),
                    "admin", serviceInstance.getHosts());
        else if (plan.getPlatform() == Platform.EXISTING_SERVICE)
            mongoDbService.createConnection(existingEndpointBean.getUsername(), existingEndpointBean.getPassword(),
                    existingEndpointBean.getDatabase(), existingEndpointBean.getHosts());

        return mongoDbService;
    }

}

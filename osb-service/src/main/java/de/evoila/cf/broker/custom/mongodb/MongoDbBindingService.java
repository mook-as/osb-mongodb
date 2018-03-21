/**
 * 
 */
package de.evoila.cf.broker.custom.mongodb;

import com.mongodb.BasicDBObject;
import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MongoDbBindingService extends BindingServiceImpl {

    @Autowired(required = false)
    private ExistingEndpointBean existingEndpointBean;

	private Logger log = LoggerFactory.getLogger(MongoDbBindingService.class);

    private static String URI = "uri";
	private static String USERNAME = "user";
    private static String PASSWORD = "password";
    private static String DATABASE = "database";

	private RandomString usernameRandomString = new RandomString(10);
    private RandomString passwordRandomString = new RandomString(15);

    @Override
    protected ServiceInstanceBinding bindService(String bindingId, ServiceInstance serviceInstance, Plan plan)
            throws ServiceBrokerException {

        List<ServerAddress> hosts = serviceInstance.getHosts();
        Map<String, Object> credentials = createCredentials(bindingId, serviceInstance, hosts, plan);

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
			List<ServerAddress> externalAddresses) throws ServiceBrokerException {

		log.debug("bind service key");
		Map<String, Object> credentials = createCredentials(bindingId, serviceInstance, externalAddresses, plan);

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
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstance serviceInstance,
                                                    ServerAddress host, Plan plan) throws ServiceBrokerException {
        List<ServerAddress> hosts = new ArrayList<>();
        hosts.add(host);

        return createCredentials(bindingId, serviceInstance, hosts, plan);
    }

    private Map<String, Object> createCredentials(String bindingId, ServiceInstance serviceInstance,
                                                    List<ServerAddress> hosts, Plan plan) throws ServiceBrokerException {
        MongoDbService mongoDbService = connection(serviceInstance, plan);

        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();
        String database = bindingId;

        MongoDBCustomImplementation.createUserForDatabase(mongoDbService, database, username, password);

        String formattedHosts = "";
        for (ServerAddress host : hosts) {
            if (formattedHosts.length() > 0)
                formattedHosts = formattedHosts.concat(",");

            formattedHosts += String.format("%s:%d", host.getIp(), host.getPort());
        }

        String dbURL = String.format("mongodb://%s:%s@%s/%s", username, password, formattedHosts, database);
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
                    existingEndpointBean.getDatabase(), existingEndpointBean.getHostsWithServerAddress());

        return mongoDbService;
    }

}

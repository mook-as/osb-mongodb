/**
 *
 */
package de.evoila.cf.broker.custom.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;

import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.cpi.existing.CustomExistingService;
import de.evoila.cf.cpi.existing.CustomExistingServiceConnection;

/**
 * @author René
 */
@Service
public class MongoDBCustomImplementation implements CustomExistingService {

    private Logger log = LoggerFactory.getLogger(MongoDBCustomImplementation.class);

    @Override
    public CustomExistingServiceConnection connection(List<String> hosts, int port, String database, String username,
                                                      String password) {

        MongoDbService mongoDbService = new MongoDbService();

        List<ServerAddress> serverAddresses = new ArrayList<>();
        for (String address : hosts) {
            ServerAddress newAddress = new ServerAddress("", address, port);
            serverAddresses.add(newAddress);
            log.info("Opening connection to " + address + ":" + port);
        }

        mongoDbService.createConnection(username, password, null, serverAddresses);

        return mongoDbService;
    }

    @Override
    public void bindRoleToInstanceWithPassword(CustomExistingServiceConnection connection, String database,
                                               String username, String password) {
        if (connection instanceof MongoDbService) {
            createUserForDatabaseWithRoles((MongoDbService) connection, database, username, password, "readWrite", "userAdmin");
        }
    }

    public static void createUserForDatabase(MongoDbService mongoDbService, String database, String username,
                                             String password) {
        createUserForDatabaseWithRoles(mongoDbService, database, username, password, "readWrite");
    }

    public static void createUserForDatabaseWithRoles(MongoDbService mongoDbService, String database, String username,
                                                      String password, String... roles) {
        Map<String, Object> commandArguments = new BasicDBObject();
        commandArguments.put("createUser", username);
        commandArguments.put("pwd", password);
        commandArguments.put("roles", roles);
        BasicDBObject command = new BasicDBObject(commandArguments);

        mongoDbService.mongoClient().getDB(database).command(command);
    }
}

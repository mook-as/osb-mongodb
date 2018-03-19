/**
 * 
 */
package de.evoila.cf.cpi.custom.props;

import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.Base64Utils;

import java.util.Map;

/**
 * @author Johannes Hiemer.
 *
 */
public class MongoDBCustomPropertyHandler {

	/**
	 * 
	 */
	private static final String TEMPLATE = "template";
	private static final String REPLICA_SET = "replicaSet";
	private static final String SECONDARY_NUMBER = "secondary_number";
	private static final String DATABASE_KEY = "database_key";

	private BytesKeyGenerator secureRandom;

	/**
	 * @param keyLength
	 */
	public MongoDBCustomPropertyHandler(int keyLength) {
		secureRandom = KeyGenerators.secureRandom(keyLength);
	}

	public Map<String, String> addDomainBasedCustomProperties(Plan plan, Map<String, String> customProperties,
			ServiceInstance serviceInstance) {

		Object replicaSetOptional = plan.getMetadata().getCustomParameters().get(REPLICA_SET);

		if (replicaSetOptional != null && replicaSetOptional instanceof String) {
			String replicaSet = (String) replicaSetOptional;

			// customProperties.put(REPLICA_SET, replicaSet);

			String templatePath = (String) plan.getMetadata().getCustomParameters().get(TEMPLATE);
			customProperties.put(TEMPLATE, templatePath);
			
			int secondaryNumber = (int) plan.getMetadata().getCustomParameters().get(SECONDARY_NUMBER);
			customProperties.put(SECONDARY_NUMBER, Integer.toString(secondaryNumber));

			String key = Base64Utils.encodeToString(secureRandom.generateKey());
			customProperties.put(DATABASE_KEY, key);
		}

		return customProperties;
	}

}

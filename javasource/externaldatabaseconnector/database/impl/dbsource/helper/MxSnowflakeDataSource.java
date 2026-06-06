package externaldatabaseconnector.database.impl.dbsource.helper;

import externaldatabaseconnector.database.constants.ErrorCode;
import externaldatabaseconnector.database.exceptions.MxInvalidFieldException;
import externaldatabaseconnector.exceptions.IMxErrorMessages;
import externaldatabaseconnector.pojo.ConnectionDetails;
import net.snowflake.client.internal.api.implementation.datasource.SnowflakeBasicDataSource;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

public class MxSnowflakeDataSource {
  private static final String END = "-----END";

  private static final String SNOWFLAKE_APPLICATION_PARAMETER = "Mendix_DbConnector";

  private static String getMultiline(String aKey) {

    // Extract the first line (beginning part)
    String firstLine = aKey.substring(0, aKey.indexOf("-----", 1) + 5);

    int indexOfEnd = aKey.lastIndexOf(END);
    // Extract the last line (ending part)
    String lastLine = aKey.substring(indexOfEnd);

    // Extract the content in between the first and last line
    String content = aKey.substring(firstLine.length(), indexOfEnd).trim();

    // Combine the parts into the desired format
    return firstLine + "\n" + content + "\n" + lastLine;
  }

  public static PrivateKey getPrivateKeyObject(String key, String passphrase)
      throws Exception {
    String formattedKey = formatPrivateKey(key);
    Security.addProvider(new BouncyCastleProvider());
    PrivateKeyInfo privateKeyInfo = parsePrivateKeyInfo(formattedKey, passphrase);
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    return converter.getPrivateKey(privateKeyInfo);
  }

  private static String formatPrivateKey(String key) throws MxInvalidFieldException {
    try {
      return getMultiline(key);
    } catch (IndexOutOfBoundsException e) {
      String messageCode = ErrorCode.PRIVATE_KEY_INVALID;
      Map<String, String> messageDataMap = new HashMap<>();
      throw new MxInvalidFieldException(IMxErrorMessages.INVALID_PRIVATE_KEY, messageCode, messageDataMap, e);
    }
  }

  private static PrivateKeyInfo parsePrivateKeyInfo(String formattedKey, String passphrase) throws Exception {
    try (PEMParser pemParser = new PEMParser(new StringReader(formattedKey))) {
      Object pemObject = pemParser.readObject();

      if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo) {
        return decryptPrivateKeyInfo((PKCS8EncryptedPrivateKeyInfo) pemObject, passphrase);
      } else if (pemObject instanceof PrivateKeyInfo) {
        return (PrivateKeyInfo) pemObject;
      }
      return null;
    }
  }

  private static PrivateKeyInfo decryptPrivateKeyInfo(PKCS8EncryptedPrivateKeyInfo encryptedInfo, String passphrase)
      throws Exception {
    InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
        .build(passphrase.toCharArray());
    return encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
  }

  public static SnowflakeBasicDataSource getSnowflakeDataSource(final ConnectionDetails connectionDetailsObject) throws Exception {
    SnowflakeBasicDataSource snowflakeBasicDataSource = new SnowflakeBasicDataSource();
    snowflakeBasicDataSource.setUser(connectionDetailsObject.getUserName());

    String connectionUrl = connectionDetailsObject.getConnectionString();
    snowflakeBasicDataSource.setUrl(connectionUrl);
    snowflakeBasicDataSource.setApplication(SNOWFLAKE_APPLICATION_PARAMETER);

    Map<String, String> additionalProperties = connectionDetailsObject.getAdditionalProperties();
    String isKeyPairAuthentication = additionalProperties.get("IsKeyPairAuthentication");

    if (isKeyPairAuthentication != null && isKeyPairAuthentication.equals("true")) {
      snowflakeBasicDataSource.setPrivateKey(getPrivateKeyObject(additionalProperties.getOrDefault("PrivateKey", ""),
          additionalProperties.getOrDefault("Passphrase", "")));
    } else {
      snowflakeBasicDataSource.setPassword(connectionDetailsObject.getPassword());
    }
    return snowflakeBasicDataSource;
  }
}

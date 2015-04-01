-- For local debugging

SET autocommit = 0;

USE blugrdev;

INSERT INTO ENVIRONMENT (ENV_ID, ENV_NAME)
VALUES
  (10, 'local');

INSERT INTO LOGICAL_DATABASE (LOGICAL_ID, LOGICAL_NAME, FK_ENV_ID)
VALUES
  (10, 'krakendb', 10);

-- FILL THIS IN WITH REAL PASSWORDS BEFORE RUNNING
INSERT INTO PHYSICAL_DATABASE (PHYSICAL_ID, IS_LIVE, DRIVER_CLASS_NAME, URL, USERNAME, `PASSWORD`, FK_LOGICAL_ID)
VALUES
  (10, 0, 'com.mysql.jdbc.Driver',
   'jdbc:mysql://127.0.0.1:3306/krakenlocal?zeroDateTimeBehavior=convertToNull', 'admin', 'password', 10);

INSERT INTO APPLICATION_VM (APPVM_ID, APPVM_SIN_NUMBER, APPVM_HOSTNAME, APPVM_IP_ADDRESS, FK_ENV_ID)
VALUES
  (10, 1, 'localhost', '127.0.0.1', 10);

INSERT INTO APPLICATION (APP_ID, FK_APPVM_ID, APP_SCHEME, APP_HOSTNAME, APP_PORT, APP_URL_PATH)
VALUES
  (10, 10, 'http', 'localhost', 8080, '/kraken/rest/administration');

COMMIT;

SET autocommit = 1;

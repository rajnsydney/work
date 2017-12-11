--This script should be run in SIT4 environment for ICC user to grant access to Onboarding stored procedures and tables
--After running this script in SIT4 once, it will be run whenever there is a change.
--Version 0.1
-- FOR PER_OWNER_SIT4 SCHEMA --

ALTER SESSION SET CURRENT_SCHEMA = PER_OWNER_SIT4;
GRANT SELECT, UPDATE, INSERT ON ONBOARDING_APPLICATION TO SRVC_ICC_ONB_SIT4;
GRANT SELECT, UPDATE, INSERT ON ONBOARDING_ACCOUNT TO SRVC_ICC_ONB_SIT4;
GRANT SELECT, UPDATE, INSERT ON ONBOARDING_PARTY TO SRVC_ICC_ONB_SIT4;
GRANT EXECUTE ON ONBOARDING_ID_UPDATES TO SRVC_ICC_ONB_SIT4;
GRANT EXECUTE ON ONBOARDING_STATUS_UPDATES TO SRVC_ICC_ONB_SIT4;
GRANT SELECT, UPDATE, INSERT ON ONBOARDING_COMMUNICATION TO SRVC_ICC_ONB_SIT4;
GRANT EXECUTE ON COMMUNICATION TO SRVC_ICC_ONB_SIT4;
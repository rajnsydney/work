REM Connect with an OS Auth account
REM CONNECT  /  AS SYSDBA;

REM Set up directories and grant access to PER_OWNER_D1 
CREATE OR REPLACE DIRECTORY &5._TAB_DIRECTORY_&6.
    AS '&1.'; 
CREATE OR REPLACE DIRECTORY &5._BAD_DIRECTORY_&6. 
    AS '&2.'; 
CREATE OR REPLACE DIRECTORY &5._LOG_DIRECTORY_&6. 
    AS '&3.'; 
CREATE OR REPLACE DIRECTORY &5._DIS_DIRECTORY_&6. 
    AS '&4.'; 

REM GRANT READ ON DIRECTORY &5._TAB_DIRECTORY TO PER_OWNER_D1; 
REM GRANT WRITE ON DIRECTORY &5._LOG_DIRECTORY TO PER_OWNER_D1; 
REM GRANT WRITE ON DIRECTORY &5._BAD_DIRECTORY TO PER_OWNER_D1;
REM GRANT WRITE ON DIRECTORY &5._DIS_DIRECTORY TO PER_OWNER_D1;
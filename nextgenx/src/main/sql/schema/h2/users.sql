drop table if exists users;

create table users(user_id varchar(20) primary key, adviser_id varchar(20),isfirsttime_loggedin	char(1), tnc_accepted char(1), tnc_accepted_on date);
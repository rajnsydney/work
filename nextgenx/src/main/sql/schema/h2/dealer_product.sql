DROP TABLE IF EXISTS dealer_products;
CREATE TABLE dealer_products (dealer_grp CHAR(255) NOT NULL, product_id CHAR(12) NOT NULL, product_name CHAR(255), ong_max_dollar NUMBER (12,2), ong_max_percent NUMBER(12,2),  lic_max_percent NUMBER(12,2), lic_max_dollar NUMBER(12,2));

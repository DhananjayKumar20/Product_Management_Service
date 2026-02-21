CREATE TABLE product (
 id INT PRIMARY KEY AUTO_INCREMENT,
 product_name VARCHAR(255) NOT NULL,
 created_by BIGINT NOT NULL,
 created_on TIMESTAMP NOT NULL,
 modified_by BIGINT
 modified_on TIMESTAMP
);
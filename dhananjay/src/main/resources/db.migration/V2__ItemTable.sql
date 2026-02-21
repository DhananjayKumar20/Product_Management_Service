CREATE TABLE item (
 id INT PRIMARY KEY AUTO_INCREMENT,
 product_id INT NOT NULL,
 quantity INT NOT NULL,
 FOREIGN KEY (product_id) REFERENCES product(id)
);

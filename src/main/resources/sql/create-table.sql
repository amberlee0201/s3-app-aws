CREATE TABLE `attachment_file` (
                                   `attachment_file_no` bigint NOT NULL AUTO_INCREMENT,
                                   `attachment_file_size` bigint DEFAULT NULL,
                                   `created_date` datetime(6) DEFAULT NULL,
                                   `modified_date` datetime(6) DEFAULT NULL,
                                   `attachment_file_name` varchar(255) DEFAULT NULL,
                                   `attachment_original_file_name` varchar(255) DEFAULT NULL,
                                   `file_path` varchar(255) DEFAULT NULL,
                                   PRIMARY KEY (`attachment_file_no`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
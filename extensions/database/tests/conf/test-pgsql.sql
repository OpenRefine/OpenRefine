USE test_db;

CREATE TABLE IF NOT EXISTS test_table (
  id integer NOT NULL,
  ue_id char(8) NOT NULL,
  start_time timestamp NOT NULL,
  end_date date DEFAULT NULL,  
  bytes_upload integer NOT NULL,
  bytes_download integer NOT NULL,
  mcc char(3) DEFAULT NULL,
  mnc char(3) NOT NULL,
  lac varchar(11) DEFAULT NULL,
  imei char(16) NOT NULL	
);

INSERT INTO test_table(id, ue_id, start_time, end_date, bytes_upload, bytes_download, mcc, mnc, lac, imei)
     VALUES (1, '11100022', now(), now(), 1024, 2048, 321, 543, 12209823498,  1344498988877487);
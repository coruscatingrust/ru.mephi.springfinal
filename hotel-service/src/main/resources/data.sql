-- Hotels
INSERT INTO HOTEL (id, name, address) VALUES (1, 'The Ritz-Carlton', 'Тверская, д. 3');
INSERT INTO HOTEL (id, name, address) VALUES (2, 'Metropol', 'Театральный проезд, 2');

-- Rooms
INSERT INTO ROOM (id, hotel_id, number, available, times_booked) VALUES (1, 1, '101', true, 0);
INSERT INTO ROOM (id, hotel_id, number, available, times_booked) VALUES (2, 1, '102', true, 2);
INSERT INTO ROOM (id, hotel_id, number, available, times_booked) VALUES (3, 2, '201', true, 1);
INSERT INTO ROOM (id, hotel_id, number, available, times_booked) VALUES (4, 2, '202', false, 5);

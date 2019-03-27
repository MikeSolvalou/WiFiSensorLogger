# WiFi Sensor Data Logger
Data measured by WiFi-connected sensors is recorded into a SQLite database. The data is served by a Jetty web server, and visualized using D3 and Google graphs.

<i>Arduino  C++  ESP8266  WiFi  OneWire<br>
Java  Jetty  Servlet  JDBC  SQL  SQLite<br>
TCP  UDP  HTTP  REST<br>
HTML  JavaScript  AJAX  XHR  D3  SVG  Google Charts</i>

## Data Graph (Google Charts)
These 3 sensors are in the same location, but sensors 1 and 2 heat up because their WiFi modules are too close (less than 2cm) to their temperature-sensing component. Sensor 3 is more than 10cm from its WiFi module, so it doesn't heat up just from being powered on. Sensors 2 and 3 are DS18B20 sensors, and sensor 1 is a DHT-11.
<img src="https://i.imgur.com/eXW6cyc.png">

## Device Diagram
<img src="https://i.imgur.com/NrCUI4s.png">

## Component Diagram
<img src="https://i.imgur.com/W66iJ0p.png">

## Database Entity Relation Diagram
<img src="https://i.imgur.com/x2mabJe.png">

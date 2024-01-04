#include <WebServer.h>
#include <WiFi.h>
#include <string>

WebServer server(80);

bool led_on = false;
int const led_pin = 12;

void setup() {
  pinMode(led_pin, OUTPUT);
  Serial.begin(115200);

  WiFi.softAP("ESP32", "", 1, true);

  Serial.print("IP address: ");
  Serial.println(WiFi.softAPIP());

  Serial.print("MAC address: ");
  Serial.println(WiFi.macAddress());

  server.on("/", []() { server.send(200, "text/plain", "Connected."); });
  server.on("/blink", []() {
    led_on = !led_on;

    if (led_on) {
      Serial.println("LED on");
      digitalWrite(led_pin, HIGH);
      server.send(200, "text/plain", "Turned on");
    } else {
      Serial.println("LED off");
      digitalWrite(led_pin, LOW);
      server.send(200, "text/plain", "Turned off");
    }
  });

  server.begin();
}

void loop() { server.handleClient(); }

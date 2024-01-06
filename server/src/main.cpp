#include <WebServer.h>
#include <WiFi.h>
#include <fstream>
#include <string>

#include <password.h>

WebServer server(80);

bool led_on = false;
int const led_pin = 12;
bool closed = true;

void setup() {
  pinMode(led_pin, OUTPUT);
  Serial.begin(115200);
  WiFi.softAP("ESP32", WIFI_PASSWORD, 1, true);

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
  server.on("/toggle", []() {
    closed = !closed;
    server.send(200, "text/plain", closed ? "Closing..." : "Opening...");

    digitalWrite(led_pin, HIGH);
    delay(1000);
    digitalWrite(led_pin, LOW);
  });

  server.begin();
}

void loop() { server.handleClient(); }

# HomeScanUI

HomeScanUI is a small home security hub. With HomeScanUI a user can use a Raspberry Pi along with a Raspberry Pi camera and Servo Motor to automatically lock/unlock a door lock upon returning/leaving home.

The code contained in this repo is for the Android Application containing the user interface for HomeScanUI. We implement multiple Amazon AWS services such as:
- Cognito for user log in
- S3 Photo Storage and Rekognition for facial recognition
- DynamoDB to keep track of lock history
- Lambda functions to automatically process data serverside
- IoT Core for storing GPS data, current lock status, and manually locking/unlocking lock

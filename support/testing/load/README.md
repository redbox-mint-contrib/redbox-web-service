# ReDBox API Web Tester

Load testing script based on [Artillery](http://artillery.io)

## Preparing
- Run `npm install` and `npm install -g artillery@1.6.0-14`
## Pre-requisites
- Start RB2 Stack
- Run `npm run build`
- Find out the `brandId` of the portal you want the records to appear. This is the `objectId` value of the portal as found in your `brandingconfig` MongoDB collection. After you retrieve this value, update the `brandId` value at [dmp-record.json](dmp-record.json).
- You are now ready to test, execute: `artillery run create-dmp-records.yml`

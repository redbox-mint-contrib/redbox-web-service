import * as faker from 'faker';

export function setJSONBody(requestParams: any, context: any, ee: any, next: any): void {
  const templateData = require('../dmp-record.json');
  templateData.metadata.title = faker.random.words();
  templateData.metadata['dc:identifier'] = faker.random.words();
  templateData.metadata['description'] = faker.lorem.paragraph();
  requestParams.json = templateData;
  return next();
}

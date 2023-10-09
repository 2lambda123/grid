/* eslint-disable no-console */
import AWS from "aws-sdk"
import { exit } from "process"
import {
  UploadStatusTableRecord,
  createQueuedUploadRecord,
  getUploadStatusTableName,
  insertNewRecord,
  scanTable,
} from "./lib/Dynamo"
import { readConfig } from "./lib/EnvironmentConfig"
import { createLogger } from "./lib/Logging"

// copy of the config code from ./index [start]
const envConfig = readConfig()
const logger = createLogger({})

const awsConfig = envConfig.isDev
  ? {
      accessKeyId: "test",
      secretAccessKey: "test",
      region: envConfig.region,
      endpoint: "https://localstack.media.local.dev-gutools.co.uk",
      s3ForcePathStyle: true,
    }
  : undefined

AWS.config.update({
  region: envConfig.region,
})
// copy of the config code from ./index [end]

const dynamoDB = new AWS.DynamoDB({
  ...awsConfig,
  apiVersion: "2012-08-10",
  // running locally need to use localhost, not the proxy
  // error using  https://localstack.media.local.dev-gutools.co.uk was:
  // code: 'NetworkingError',
  // Error: unable to verify the first certificate
  endpoint: envConfig.isDev ? "http://localhost:4566" : undefined,
})

// TO DO - check the actual inputs from the `ImportAction`s
// will they actually provide the context needed to create the record
// with the right expected ID?
// We know where the files is in the ingest bucket, but not where the
// file is supposed to end up when uploaded
const testRecord = createQueuedUploadRecord(
  {
    "file-name": 'image with real id.tiff',
    "upload-image-id": '0d47e46edd20b70699429b0c9bff89b1082ba36b'
  },
  "image with real id.tiff",
  new Date("2030-01-01").valueOf()
)

/** Test operation - log the records in the upload table */
const logAllRecords = async () => {
  const tableName = await getUploadStatusTableName(logger, dynamoDB, envConfig)
  const results = await scanTable(logger, dynamoDB, tableName, 10)
  console.log(
    {
      tableName,
      count: results?.length,
    },
    results
  )
}

/** Test operation - add a record to the upload table */
const putInARecord = async (recordToPut: UploadStatusTableRecord) => {
  const tableName = await getUploadStatusTableName(logger, dynamoDB, envConfig)
  await insertNewRecord(logger, dynamoDB, tableName, recordToPut)
}

putInARecord(testRecord)
  .then(logAllRecords)
  .catch((err) => {
    console.warn(err)
  })
  .finally(exit)

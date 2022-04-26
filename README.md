# report-processor

Report processor framework implementation. 

A report processor monitors folders for file changes. 
If an XML file is created or updated in a monitored folder, 
the respective report handlers are notified, based on the report type. 

Each folder is associated with one or many report types. 
Each handler is associated with one or many report types.

Report processor supports
* adding a monitored folder with associated report types
* removing a folder from monitoring
* registering a handler with associated report types
* unregistering a handler
* start
* graceful shutdown (handle all the existing files)
* forced shutdown (some existing file might not be handled)

When a folder is removed from monitoring, all the existing files in it are handled. 

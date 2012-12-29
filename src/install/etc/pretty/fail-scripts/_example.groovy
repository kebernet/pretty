// Bring up the new virtual interface.
"ifup eth0:1".execute();
// Restart the HTTP service so it will bind to the new interface
Server.stop();
Server.start();

package tcs.employee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.sun.jersey.multipart.FormDataParam;

import org.apache.log4j.Logger;

import com.sun.jersey.core.header.FormDataContentDisposition;

@Path("/files")
public class EmployeeData {
	static Logger logger = Logger.getLogger(EmployeeData.class);
	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("text/html")
	public Response uploadFile(
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileInputDetails,
			@FormDataParam("id") String  id,
			@FormDataParam("description") String  description,
			@FormDataParam("file_year") String file_year,
			@FormDataParam("department") String department) throws UnknownHostException {
 
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		DB mongoDB = mongoClient.getDB("tutorial");
		
		//Let's store the standard data in regular collection
		DBCollection collection = mongoDB.getCollection("filestore");
		
		BasicDBObject query = new BasicDBObject();
        query.put("_id", id);
        DBCursor cursor = collection.find(query);
		
		if (!cursor.hasNext()) {
			// Build our document and add all the fields
			BasicDBObject document = new BasicDBObject();
			document.append("_id", id);
			document.append("description", description);
			document.append("file_year", file_year);
			document.append("filename", fileInputDetails.getFileName());
			document.append("department", department);
			
			//insert the document into the collection 
			collection.insert(document);
			
			// Now let's store the binary file data using filestore GridFS  
			GridFS fileStore = new GridFS(mongoDB, "filestore");
	        GridFSInputFile inputFile = fileStore.createFile(fileInputStream);
	        inputFile.setId(id);
	        inputFile.setFilename(fileInputDetails.getFileName());
	        inputFile.save();
	        
			String status = "Upload has been successful";
		
			logger.info("ID: " + id);
			logger.info("description: " + description);
			logger.info("department: " + department);
			logger.info("file_year: : " + file_year);
			logger.info("fileInputDetails: " + fileInputDetails);
			
			return Response.status(200).entity(status).build();
		} else {
			String status = "Unable to insert record with ID: " + id +" as record already exists!!!";
			logger.info(status);
			return Response.status(200).entity(status).build();
		}
	}
	
	@GET
	@Path("/download/file/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadFilebyID(@PathParam("id")  String id) throws IOException {
		
		Response response = null;
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		DB mongoDB = mongoClient.getDB("tutorial");
		
		//Let's store the standard data in regular collection
		DBCollection collection = mongoDB.getCollection("filestore");
		
		logger.info("Inside downloadFilebyID...");
		logger.info("ID: " + id);

		BasicDBObject query = new BasicDBObject();
        query.put("_id", id);
        DBObject doc = collection.findOne(query);
        DBCursor cursor = collection.find(query);
        
        if (cursor.hasNext()) {
        	
	        Set<String> allKeys = doc.keySet();
	        HashMap<String, String> fields = new HashMap<String,String>();
	        for (String key: allKeys) {
	        	fields.put(key, doc.get(key).toString());
	        }
	        
			logger.info("description: " + fields.get("description"));
			logger.info("department: " + fields.get("department"));
			logger.info("file_year: " + fields.get("file_year"));
			logger.info("filename: " + fields.get("filename"));
	       
			GridFS fileStore = new GridFS(mongoDB, "filestore");
	        GridFSDBFile gridFile = fileStore.findOne(query);
	
	        InputStream in = gridFile.getInputStream();
					
			ByteArrayOutputStream out = new ByteArrayOutputStream();
		    int data = in.read();
		    while (data >= 0) {
		      out.write((char) data);
		      data = in.read();
		    }
			out.flush();
			
	
			ResponseBuilder builder = Response.ok(out.toByteArray());
			builder.header("Content-Disposition", "attachment; filename=" + fields.get("filename"));
			response = builder.build();
			
        } else {
        	response = Response.status(404).
				      entity(" Unable to get file with ID: " + id).
				      type("text/plain").
				      build();
        }
        
		return response;
	}
	
	@GET
	@Path("/download/details/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response showFileStoreDetails(@PathParam("id")  String id) throws UnknownHostException {
		
		Response response = null;
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		DB mongoDB = mongoClient.getDB("tutorial");
		
		//Let's store the standard data in regular collection
		DBCollection collection = mongoDB.getCollection("filestore");
		
		logger.info("Inside showFileStoreDetails...");
		logger.info("ID: " + id);

		BasicDBObject query = new BasicDBObject();
        query.put("_id", id);
        DBObject doc = collection.findOne(query);

        DBCursor cursor = collection.find(query);
        
        if (cursor.hasNext()) {
	        Set<String> allKeys = doc.keySet();
	        HashMap<String, String> fields = new HashMap<String,String>();
	        for (String key: allKeys) {
	        	fields.put(key, doc.get(key).toString());
	        }
	        
			logger.info("description: " + fields.get("description"));
			logger.info("department: " + fields.get("department"));
			logger.info("file_year: " + fields.get("file_year"));
			logger.info("filename: " + fields.get("filename"));
			
			StringBuffer status = new StringBuffer("Inside showHeaders: <br/><br/>");
			status.append("description : ");
			status.append(fields.get("description"));
			status.append("<br/>");
			status.append("department : ");
			status.append(fields.get("department"));
			status.append("<br/>");
			status.append("file_year : ");
			status.append(fields.get("file_year"));
			status.append("<br/>");
			status.append("filename : ");
			status.append(fields.get("filename"));
			status.append("<br/>");
			
			response = Response.status(200).entity(status.toString()).build();
        } else {
        	response = Response.status(404).
				      entity(" Unable to get file with ID: " + id).
				      type("text/plain").
				      build();
        }
		return response;
	}
	
}

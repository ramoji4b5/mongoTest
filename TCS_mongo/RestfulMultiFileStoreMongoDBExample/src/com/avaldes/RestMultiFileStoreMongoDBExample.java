package com.avaldes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

@Path("/files")
public class RestMultiFileStoreMongoDBExample {
	static Logger logger = Logger.getLogger(RestMultiFileStoreMongoDBExample.class);
	
	@GET
	@Path("/status")
	@Produces(MediaType.TEXT_HTML)
	public Response status() {
		String msg = String.format("Server Status is OK");
		logger.info(msg);
		 
 		return Response.status(200).entity(msg).build();
	}	
	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("text/html")
	public Response uploadFile (
			@Context HttpServletRequest req
			) throws UnknownHostException, Exception {
 
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		DB mongoDB = mongoClient.getDB("tutorial");
		//Let's store the standard data in regular collection
		
		if (ServletFileUpload.isMultipartContent(req)) {
			logger.info("We have received MultiPart Request...");
			FileItemFactory fiFactory = new DiskFileItemFactory();
			ServletFileUpload fileUpload = new ServletFileUpload(fiFactory);
			
			List<FileItem> listItems = fileUpload.parseRequest(req);
			Iterator<FileItem> iter = listItems.iterator();
								
			GridFS fileStore = new GridFS(mongoDB, "filestore");				
			while (iter.hasNext()) {
				FileItem item = iter.next();
	      
				if (!item.isFormField()) {	
					InputStream in=item.getInputStream();
		      
					logger.info("Filename.....: " + item.getName());
					logger.info("File Size....: " + item.getSize());
					logger.info("File Type....: " + item.getContentType());	      
					  
					GridFSInputFile inputFile = fileStore.createFile(in);
					inputFile.setId(item.getName());
					inputFile.setFilename(item.getName());
					inputFile.save();
					in.close();
		      }
			}
		}
			
		String status = "Upload has been successful";

		return Response.status(200).entity(status).build();
	}

	@GET
	@Path("/download/file/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadFilebyID(@PathParam("id")  String id) throws IOException {
		
		Response response = null;
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		DB mongoDB = mongoClient.getDB("tutorial");

		logger.info("Inside downloadFilebyID...");
		logger.info("ID: " + id);

		BasicDBObject query = new BasicDBObject();
		query.put("_id", id);
		GridFS fileStore = new GridFS(mongoDB, "filestore");
		GridFSDBFile gridFile = fileStore.findOne(query);

		if (gridFile != null && id.equalsIgnoreCase((String)gridFile.getId())) {
			logger.info("ID........: " + gridFile.getId());
			logger.info("FileName..: " + gridFile.getFilename());
			logger.info("Length....: " + gridFile.getLength());
			logger.info("Upload Date..: " + gridFile.getUploadDate());
			
			InputStream in = gridFile.getInputStream();
					
			ByteArrayOutputStream out = new ByteArrayOutputStream();
		    int data = in.read();
		    while (data >= 0) {
		      out.write((char) data);
		      data = in.read();
		    }
			out.flush();
	
			ResponseBuilder builder = Response.ok(out.toByteArray());
			
			builder.header("Content-Disposition", "attachment; filename=" + gridFile.getFilename());
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
		
		BasicDBObject query = new BasicDBObject();
		query.put("_id", id);
		GridFS fileStore = new GridFS(mongoDB, "filestore");
		GridFSDBFile gridFile = fileStore.findOne(query);

		if (gridFile != null && id.equalsIgnoreCase((String)gridFile.getId())) {
			logger.info("ID...........: " + gridFile.getId());
			logger.info("FileName.....: " + gridFile.getFilename());
			logger.info("Length.......: " + gridFile.getLength());
			logger.info("Upload Date..: " + gridFile.getUploadDate());

			StringBuffer status = new StringBuffer("<pre>Inside showHeaders: <br/><br/>");
			status.append("ID...........: ");
			status.append(gridFile.getId());
			status.append("<br/>");
			status.append("FileName.....: ");
			status.append(gridFile.getFilename());
			status.append("<br/>");
			status.append("Length.......: ");
			status.append(gridFile.getLength());
			status.append("<br/>");
			status.append("Upload Date..: ");
			status.append(gridFile.getUploadDate());
			status.append("<br/></pre>");
			
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

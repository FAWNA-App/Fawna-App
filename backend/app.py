from fastapi import FastAPI, HTTPException
from pymongo import MongoClient
from dotenv import load_dotenv
import os
import logging
import uvicorn
from bson import json_util
import json

# Set up logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

load_dotenv()

app = FastAPI()

@app.on_event("startup")
def startup_db_client():
    mongodb_uri = os.getenv("MONGODB_URI")
    logger.debug(f"MongoDB URI found: {'Yes' if mongodb_uri else 'No'}")
    
    if not mongodb_uri:
        logger.warning("MONGODB_URI not found in environment variables.")
        app.mongodb_client = None
        app.mongodb = None
        return

    try:
        app.mongodb_client = MongoClient(mongodb_uri)
        
        # Explicitly set the database to sample_mflix
        app.mongodb = app.mongodb_client.sample_mflix
        
        # Test the connection
        app.mongodb_client.admin.command('ping')
        
        logger.info(f"Successfully connected to MongoDB! Database: {app.mongodb.name}")
    except Exception as e:
        logger.error(f"Failed to connect to MongoDB: {str(e)}")
        app.mongodb_client = None
        app.mongodb = None

@app.on_event("shutdown")
def shutdown_db_client():
    if app.mongodb_client:
        app.mongodb_client.close()
        logger.info("MongoDB connection closed.")

@app.get("/")
async def root():
    logger.info("Root endpoint accessed")
    return {"message": "Hello World"}

@app.get("/test_db")
async def test_db():
    logger.info("Test DB endpoint accessed")
    if not app.mongodb_client:
        logger.error("MongoDB client is not initialized")
        raise HTTPException(status_code=500, detail="MongoDB connection not established")
    
    try:
        # Use the existing connection to ping
        app.mongodb_client.admin.command('ping')
        return {"message": f"Successfully connected to MongoDB! Database: {app.mongodb.name}"}
    except Exception as e:
        logger.error(f"Failed to ping MongoDB: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to connect to MongoDB: {str(e)}")

@app.get("/mflix_comments")
async def get_mflix_comments():
    logger.info("Mflix comments endpoint accessed")
    if not app.mongodb:
        raise HTTPException(status_code=500, detail="MongoDB connection not established")
    
    try:
        # Fetch 5 comments from the 'comments' collection in sample_mflix
        comments = list(app.mongodb.comments.find({}, {"name": 1, "text": 1, "_id": 0}).limit(5))
        
        # Convert to JSON-serializable format
        comments_json = json.loads(json_util.dumps(comments))
        
        return {"comments": comments_json}
    except Exception as e:
        logger.error(f"Failed to fetch comments: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch comments: {str(e)}")

@app.on_event("startup")
async def startup_event():
    routes = [{"path": route.path, "name": route.name} for route in app.routes]
    logger.info(f"Registered routes: {routes}")

if __name__ == "__main__":
    logger.info("Starting the application")
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level="debug")
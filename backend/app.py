import os
from fastapi import FastAPI, HTTPException
from pymongo import MongoClient
from dotenv import load_dotenv
import logging
from urllib.parse import urlparse

# Set up logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

load_dotenv()

app = FastAPI()

def get_database_name(uri):
    parsed_uri = urlparse(uri)
    database_name = parsed_uri.path.lstrip('/')
    return database_name if database_name else None

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
        database_name = get_database_name(mongodb_uri)
        logger.debug(f"Extracted database name: {database_name}")

        if not database_name:
            logger.warning("No database name found in URI. Using default.")
            database_name = 'default_database'

        app.mongodb_client = MongoClient(mongodb_uri)
        app.mongodb = app.mongodb_client[database_name]
        
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
def root():
    return {"message": "Hello World"}

@app.get("/test_db")
def test_db():
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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
    
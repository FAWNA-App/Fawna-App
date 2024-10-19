from fastapi import FastAPI
from motor.motor_asyncio import AsyncIOMotorClient
from dotenv import load_dotenv
import os

load_dotenv()

app = FastAPI()

@app.on_event("startup")
async def startup_db_client():
    mongodb_uri = os.getenv("MONGODB_URI")
    if not mongodb_uri:
        print("WARNING: MONGODB_URI not found in environment variables.")
        app.mongodb_client = None
        app.mongodb = None
    else:
        try:
            app.mongodb_client = AsyncIOMotorClient(mongodb_uri)
            app.mongodb = app.mongodb_client.get_default_database()
            print(f"Connected to MongoDB! Database: {app.mongodb.name}")
        except Exception as e:
            print(f"Failed to connect to MongoDB: {e}")
            app.mongodb_client = None
            app.mongodb = None

@app.on_event("shutdown")
async def shutdown_db_client():
    if app.mongodb_client:
        app.mongodb_client.close()

@app.get("/")
async def root():
    return {"message": "Hello World"}

@app.get("/test_db")
async def test_db():
    if not app.mongodb:
        return {"error": "MongoDB connection not established"}
    try:
        await app.mongodb.command("ping")
        return {"message": f"Successfully connected to MongoDB! Database: {app.mongodb.name}"}
    except Exception as e:
        return {"error": f"Failed to connect to MongoDB: {str(e)}"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
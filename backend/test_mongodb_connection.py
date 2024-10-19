from pymongo.mongo_client import MongoClient
from dotenv import load_dotenv
import os

# Load environment variables from .env file
load_dotenv()

# Get the MongoDB URI from the environment variable
uri = os.getenv("MONGODB_URI")

if not uri:
    print("ERROR: MONGODB_URI not found in environment variables.")
else:
    # Create a new client and connect to the server
    client = MongoClient(uri)

    # Send a ping to confirm a successful connection
    try:
        client.admin.command('ping')
        print("Pinged your deployment. You successfully connected to MongoDB!")
    except Exception as e:
        print(f"An error occurred while connecting to MongoDB: {e}")

    # Don't forget to close the connection when you're done
    client.close()
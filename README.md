SRM Campus Marketplace

A simple full-stack web application built for SRM students to list items for sale or rent. Users can register, log in, view listings, post new listings (including contact info), and delete their own listings.

Technologies Used

Backend:

Java 17+

Spring Boot 3+ (Web, Data JPA, Security)

Maven (for project management and dependencies)

PostgreSQL (Database)

Frontend:

HTML

Tailwind CSS (for styling)

Vanilla JavaScript (for interactivity and API calls)

Database:

PostgreSQL

Development Tools:

IntelliJ IDEA (or any Java IDE)

Git & GitHub (Version Control)

pgAdmin (Database management)

Setup Instructions

Prerequisites:

Java JDK (17 or newer) installed.

Git installed.

PostgreSQL installed and running.

An IDE like IntelliJ IDEA or VS Code (with Java extensions).

Clone the Repository:

git clone [https://github.com/rnathadithya12-code/srm-campus-marketplace.git](https://github.com/rnathadithya12-code/srm-campus-marketplace.git)
cd srm-campus-marketplace


Create the Database:

Open pgAdmin or use the psql command line.

Run the following SQL command:

CREATE DATABASE student_marketplace;


Configure Backend:

Open the project in your IDE.

Navigate to src/main/resources/application.properties.

Update the following lines with your PostgreSQL username and password:

spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password


Running the Application

Run the Backend:

In your IDE, find the MarketplaceApplication.java file located at src/main/java/com/example/studentmarketplace/MarketplaceApplication.java.

Right-click on the file and select "Run 'MarketplaceApplication.main()'".

The server will start on http://localhost:8080. The first time it runs, it will automatically create the necessary database tables (users, listings).

Run the Frontend:

Navigate to the project folder (srm-campus-marketplace) on your computer.

Open the index.html file in your web browser (using a tool like VS Code's "Live Server" is recommended to avoid potential issues).

The website will load and connect to the running backend server.

Using the App:

You must Register a new account first (including a phone number).

Then Login with your new credentials.

You can now Create Listings, view All Listings, view My Listings, and Delete your own listings from the "My Listings" tab.
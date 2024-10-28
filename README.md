# CineCast: Video Streaming Platform

CineCast is a comprehensive video streaming platform developed using Angular, Java, Spring Boot, and MySQL. It supports video upload and adaptive streaming across multiple resolutions, ensuring a smooth viewing experience.

## Technologies Used

- **Frontend:** Angular
- **Backend:** Java, Spring Boot
- **Database:** MySQL
- **Streaming:** HLS (HTTP Live Streaming)
- **Containerization:** Docker
- **Video Processing:** ffmpeg

## Getting Started

To run the CineCast application locally, follow these steps:

### Prerequisites

- **Docker**: Ensure you have Docker installed on your machine.
- **Java JDK**: Make sure you have Java JDK (version 8 or higher) installed.
- **Node.js**: Install Node.js (version 12 or higher) for running the Angular application.

### Step 1: Clone the Repository

Clone the repository to your local machine:

```bash
git clone https://github.com/Euphoria99/cinecast-springboot.git
cd CineCast
```

### Step 2: Set Up the MySQL Database
Run the following Docker command to start the MySQL container:

```
docker run --name video-streaming -p 3306:3306 -e MYSQL_DATABASE=video_db -e MYSQL_ROOT_PASSWORD=Pavan@123 -d mysql
```

### Step 3: Configure the Backend

Navigate to the backend directory (where your Spring Boot application is located).
Update the `application.properties` or `application.yml` file with your MySQL database connection details if necessary.
properties

```
spring.datasource.url=jdbc:mysql://localhost:3306/video_db
spring.datasource.username=root
spring.datasource.password=Pavan@123
```
 make sure to replace with your `username` and `password` 


## Frontend 

Check this [repo](https://github.com/Euphoria99/video-streaming-angular) for frontend.
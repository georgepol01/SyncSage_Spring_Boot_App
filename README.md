![syncsagelogo](https://github.com/user-attachments/assets/a6944917-057e-472e-a851-129b0fc8e4a3)

## About

**SyncSage** is an advanced automation tool designed to streamline the management of listing availability across multiple booking platforms, including Booking.com and Airbnb. By monitoring and processing emails associated with these platforms, SyncSage ensures that your listing availability is consistently synchronized, reducing the risk of double bookings and improving overall efficiency.

##**Key Features**:
-**Email Monitoring and Processing**: SyncSage intelligently scans and interprets incoming emails from various booking platforms to detect updates to your listing availability. This process is fully automated, requiring no manual intervention.

-**Cross-Platform Synchronization**: The tool automatically adjusts your availability settings across different platforms, ensuring that any changes on one platform are reflected on all others. This helps prevent double bookings and ensures that your listings are always up-to-date.

-**Seamless Integration: Built with Spring Boot, SyncSage integrates effortlessly into your existing infrastructure, providing a robust and scalable solution for property managers who need to handle multiple listings across various platforms.

-**Error Handling and Reporting**: SyncSage includes comprehensive error handling mechanisms to manage any inconsistencies or issues that may arise during the synchronization process. Detailed logs and reports are generated to help you stay informed and address potential problems quickly.

-**User-Friendly Configuration**: The tool offers easy-to-configure settings, allowing users to tailor the synchronization process to their specific needs. Whether you’re managing a single property or a large portfolio, SyncSage adapts to your requirements.

##**Use Cases**:
-**Property Managers**: Ideal for property managers who need to keep track of availability across several booking platforms, reducing the administrative burden and minimizing the chances of errors.

-**Vacation Rental Owners**: Perfect for vacation rental owners who list their properties on multiple platforms and want to ensure their availability is always accurate and up-to-date.

##**How It Works**:
1. **Email Parsing**: SyncSage monitors incoming emails from your linked booking platforms and extracts relevant data such as booking dates, cancellations, and availability updates.

2. **Data Synchronization**: The extracted data is processed and used to update your listings across all connected platforms, ensuring consistency and accuracy.

3. **Logging and Alerts**: All actions are logged, and in case of any issues during synchronization, alerts are generated so you can take immediate action.

4. **Technology**: SyncSage utilizes Selenium for automated web interactions, ensuring accurate and efficient updates to your listings. Please note that the current version of SyncSage is specifically designed to work with Airbnb.

##**Instructions for Setup and Usage**:
Follow these steps to set up and run SyncSage:

1. **Install Java 21**: Ensure that Java 21 is installed on your system. This is a prerequisite for running the application.

2. **Download the Repository**: Clone or download the SyncSage repository from your version control system.

3. **Use an IDE**: Open the project in an Integrated Development Environment (IDE) like IntelliJ IDEA for easier management and configuration.

4. ###**Configure application.properties**:

  -Use the application.properties.template provided in the repository to create a new application.properties file.
  -Fill in the necessary values under #Email Monitoring Service Settings.
  -Ensure that email.provider.url=https://www.gmx.com/ remains unchanged, as SyncSage currently only supports GMX email.
  -Create a GMX Email Account: If you don’t already have one, create a GMX email account and link it to your listing platform (e.g., Booking.com).
  -Set email.sender: Enter the sender's name for the confirmation emails sent by the booking platform when a booking is confirmed.
  -Set GMX Email Credentials: Provide the gmx.email and gmx.password values with the credentials of your GMX email account.
  -Set airbnb.listing.names: List the names of the Airbnb listings you manage, as these will be monitored by SyncSage.
  
5. ###**Download and Configure ChromeDriver**:

  -Download the latest version of ChromeDriver and ensure your Google Chrome browser is updated to the latest version.
  -Make sure that the ChromeDriver version is compatible with your version of Chrome.
  
6. ###**Adjust the Scheduler** (Optional):

  -In the EmailMonitoringService class, you can customize how often the email monitoring scheduler runs. For example, adjust the interval with @Scheduled(fixedDelay = 10000) to set the desired delay in milliseconds.

7. ###**Build and Run the Application**:

  -After setting up all configurations, build the application using your IDE or a command-line tool.
  -Run the application, and SyncSage will begin monitoring emails and synchronizing your listings automatically.

Please note that you'll need to manually enter your Airbnb credentials during the process and ensure any pop-ups on GMX.com are closed, as the app relies on the Chrome profile for maintaining session information.


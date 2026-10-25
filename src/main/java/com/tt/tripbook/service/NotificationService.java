package com.tt.tripbook.service;

import com.tt.tripbook.model.Trip;
import com.tt.tripbook.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Sends departure reminders via email (SMTP through SendGrid).
// Triggered by POST /api/notify, which is called by an external cron service (e.g. cron-job.org)
// every day. The endpoint is protected by a shared secret token, not by JWT.
@Service
public class NotificationService {

    private static final int DAYS_AHEAD = 3;

    private final TripRepository tripRepository;
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public NotificationService(TripRepository tripRepository,
                               JavaMailSender mailSender,
                               @Value("${MAIL_FROM:noreply@tripbook.app}") String fromAddress) {
        this.tripRepository = tripRepository;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    // Find all CONFIRMED trips whose departure date is exactly DAYS_AHEAD days from today,
    // and send a reminder to each passenger's email address.
    @Transactional(readOnly = true)
    public int sendDepartureReminders() {
        LocalDate target = LocalDate.now().plusDays(DAYS_AHEAD);
        List<Trip> trips = tripRepository.findByStatusAndDepartureDateWithPassengerAndDestination(target);

        for (Trip trip : trips) {
            String to = trip.getPassenger().getEmail();
            String passengerName = trip.getPassenger().getName();
            String city = trip.getDestination().getCity();
            String country = trip.getDestination().getCountry();

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject("TripBook — Your trip to " + city + " is in " + DAYS_AHEAD + " days");
            msg.setText(
                    "Hi " + passengerName + ",\n\n" +
                    "This is a reminder that your trip to " + city + ", " + country +
                    " departs on " + trip.getDepartureDate() + ".\n\n" +
                    "Safe travels!\n— TripBook"
            );

            mailSender.send(msg);
        }

        return trips.size();
    }
}

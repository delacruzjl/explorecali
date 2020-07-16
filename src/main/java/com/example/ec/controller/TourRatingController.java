package com.example.ec.controller;

import com.example.ec.domain.*;
import com.example.ec.model.RatingDto;
import com.example.ec.repo.*;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tours/{tourId}/ratings")
public class TourRatingController {

    TourRatingRepository tourRatingRepository;
    TourRepository tourRepository;
    TelemetryClient telemetryClient;

    @Autowired
    public TourRatingController(TourRatingRepository tourRatingRepository, TourRepository tourRepository, TelemetryClient telemetryClient) {
        this.tourRatingRepository = tourRatingRepository;
        this.tourRepository = tourRepository;
        this.telemetryClient = telemetryClient;
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public TourRating createTourRating(@PathVariable(value = "tourId") int tourId, @RequestBody @Validated RatingDto ratingDto) {
        // tour must exist otherwise return 404, not found
        Tour tour = verifyTour(tourId);

        // instantiate new TourRating
        TourRating tourRating = tourRatingRepository.save(
                new TourRating( new TourRatingPk(tour, ratingDto.getCustomerId()), ratingDto.getScore(), ratingDto.getComment() ));

        // persist the new TourRating
        //return 201, created
        return tourRating;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Page<RatingDto> getAllRatingForTour(@PathVariable(value = "tourId") int tourId, Pageable pageable) {

        //track a custom event
        telemetryClient.trackEvent("Sending a custom event...");

        //trace a custom trace
        telemetryClient.trackTrace("Sending a custom trace....");

        //track a custom metric
        telemetryClient.trackMetric("custom metric", 1.0);

        //track a custom dependency
        telemetryClient.trackDependency("SQL", "Insert", new Duration(0, 0, 1, 1, 1), true);


        Tour tour = verifyTour(tourId);
        Page<TourRating> tourRatings = tourRatingRepository.findByPkTourId(tour.getId(), pageable);

        List<RatingDto> ratingDtos = tourRatings.getContent().stream().map(tourRating ->
                toDto(tourRating)).collect(Collectors.toList());

        return new PageImpl<>(ratingDtos, pageable, tourRatings.getTotalPages());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/average")
    public AbstractMap.SimpleEntry<String, Double> getAverage(@PathVariable(value = "tourId") int tourId) {
        Tour tour = verifyTour(tourId);
        List<TourRating> tourRatings = tourRatingRepository.findByPkTourId(tour.getId());

        OptionalDouble average = tourRatings.stream().mapToInt(TourRating::getScore).average();
        return new AbstractMap.SimpleEntry<String, Double>("average", average.isPresent() ? average.getAsDouble():null);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public RatingDto updateWithPut(@PathVariable(value = "tourId") int tourId, @RequestBody @Validated RatingDto ratingDto) {
        TourRating tourRating = verifyTourRating(tourId, ratingDto.getCustomerId());
        tourRating.setScore(ratingDto.getScore());
        tourRating.setComment(ratingDto.getComment());
        return toDto(tourRatingRepository.save(tourRating));
    }

    @RequestMapping(method = RequestMethod.PATCH)
    public RatingDto updateWithPatch(@PathVariable(value = "tourId") int tourId, @RequestBody @Validated RatingDto ratingDto) {
        TourRating tourRating = verifyTourRating(tourId, ratingDto.getCustomerId());

        if (ratingDto.getScore() != null)        tourRating.setScore(ratingDto.getScore());
        if (ratingDto.getComment() != null)        tourRating.setComment(ratingDto.getComment());
        return toDto(tourRatingRepository.save(tourRating));
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/{customerId}")
    public void delete(@PathVariable(value = "tourId") int tourId, @PathVariable(value = "customerId") int customerId) {
        TourRating tourRating = verifyTourRating(tourId, customerId);
        tourRatingRepository.delete(tourRating);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSuchElementException.class)
    public String return404(NoSuchElementException ex) {
        return ex.getMessage();
    }

    private Tour verifyTour(int tourId) throws NoSuchElementException {
        return tourRepository.findById(tourId)
                .orElseThrow(() -> new NoSuchElementException("Tour doesn't exists: " + tourId));
    }

    private RatingDto toDto(TourRating tourRating) {
        return new RatingDto(
                tourRating.getScore(),
                tourRating.getComment(),
                tourRating.getPk().getCustomerId());
    }

    private TourRating verifyTourRating(int tourId, int customerId) {
        return tourRatingRepository.findByPkTourIdAndPkCustomerId(tourId, customerId)
                .orElseThrow(() -> new NoSuchElementException(
                        new Formatter().format("Tour Rating doesn't exists: %d %d", tourId, customerId ).toString()
                ));
    }

}

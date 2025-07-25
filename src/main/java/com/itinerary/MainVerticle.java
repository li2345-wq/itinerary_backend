package com.itinerary;

import com.itinerary.handlers.AuthHandler;
import com.itinerary.handlers.TripHandler;
import com.itinerary.handlers.WebHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashSet;
import java.util.Set;
import io.github.cdimascio.dotenv.Dotenv;



public class MainVerticle extends AbstractVerticle {

    // adding this to make frontend *easier
   /*  Dotenv dotenv = Dotenv.load();
    String mongoUri = dotenv.get("MONGO_URI");
    String jwtSecret = dotenv.get("JWT_SECRET_KEY");*/
    // the frontend get me into insane ....
    String mongoUri = System.getenv("MONGO_URI");
    String jwtSecret = System.getenv("JWT_SECRET_KEY");

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject mongoConfig = new JsonObject()
                .put("connection_string",mongoUri)
                .put("ssl", true)
                .put("trustAll", true)
                .put("db_name", "itinerary_app");

        MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);

        // Simplified JWT configuration for Vert.x 4.5.1
        JWTAuthOptions jwtOptions = new JWTAuthOptions()
                .addJwk(new JsonObject()
                        .put("kty", "oct")
                        .put("k", jwtSecret));

        JWTAuth jwtAuth = JWTAuth.create(vertx, jwtOptions);

        Router router = Router.router(vertx);

        // Enable CORS
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("Authorization");

        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders));
        router.route().handler(BodyHandler.create());

        AuthHandler authHandler = new AuthHandler(mongoClient, jwtAuth);
        TripHandler tripHandler = new TripHandler(mongoClient);
        WebHandler webHandler = new WebHandler(vertx); // Fixed: passing vertx parameter

        // Auth routes
        router.post("/auth/signup").handler(authHandler::signup);
        router.post("/auth/login").handler(authHandler::login);

        // Static test pages
        router.get("/test-login").handler(webHandler::serveLoginPage);
        router.get("/test-signup").handler(webHandler::serveSignupPage);
        router.get("/test-dashboard").handler(webHandler::serveDashboardPage);
        router.get("/health").handler(ctx -> {
            ctx.response()
                    .putHeader("content-type", "application/json")
                    .end("{\"status\":\"UP\"}");
        });

        // Static resources
        router.route("/css/*").handler(StaticHandler.create("webroot"));
        router.route("/js/*").handler(StaticHandler.create("webroot"));

        // Secure API
        router.route("/api/*").handler(JWTAuthHandler.create(jwtAuth));

        // Trip routes
        router.get("/api/dashboard").handler(tripHandler::getAllTrips);
        router.post("/api/trips").handler(tripHandler::createTrip);
        router.get("/api/trips/:tripId").handler(tripHandler::getTripById); // ADDED: Get specific trip
        router.put("/api/trips/:tripId").handler(tripHandler::updateTrip);   // ADDED: Update trip
        router.delete("/api/trips/:tripId").handler(tripHandler::deleteTrip);

        // Day routes
        router.post("/api/trips/:tripId/days").handler(tripHandler::addDay);
        router.put("/api/trips/:tripId/days/:dayNumber").handler(tripHandler::updateDay);
        router.delete("/api/trips/:tripId/days/:dayNumber").handler(tripHandler::deleteDay);

        // Activity routes
        router.post("/api/trips/:tripId/days/:dayNumber/activities").handler(tripHandler::addActivity);
        router.put("/api/trips/:tripId/days/:dayNumber/activities/:activityName").handler(tripHandler::updateActivity);
        router.delete("/api/trips/:tripId/days/:dayNumber/activities/:activityName").handler(tripHandler::deleteActivity);

        // Reorder activities within a day
        router.put("/api/trips/:tripId/days/:dayNumber/reorder").handler(tripHandler::reorderActivities);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8888"));


        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, "0.0.0.0")
                .onSuccess(server -> {
                    System.out.println("HTTP server started on port " + server.actualPort());
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }
}
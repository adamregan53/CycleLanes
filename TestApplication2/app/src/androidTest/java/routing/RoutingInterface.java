package routing;





import java.util.List;

public interface RoutingInterface {
    void onRoutingFailure(RouteException e);

    void onRoutingStart();

    void onRoutingSuccess(List<Route> route, int shortestRouteIndex);

    void onRoutingCancelled();
}

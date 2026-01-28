package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Exception thrown when a position is outside map bounds.
 */
public class PositionOutOfBoundsException extends RuntimeException {

    private final Position position;
    private final GridMap gridMap;

    public PositionOutOfBoundsException(Position position, GridMap gridMap) {
        super(String.format("Position (%.1f, %.1f, %.1f) is out of bounds for map of size %dx%dx%d",
                position.x(), position.y(), position.z(),
                gridMap.width(), gridMap.height(), gridMap.depth()));
        this.position = position;
        this.gridMap = gridMap;
    }

    public Position getPosition() {
        return position;
    }

    public GridMap getMap() {
        return gridMap;
    }
}

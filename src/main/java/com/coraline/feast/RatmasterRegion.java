package com.coraline.feast;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Arena-local convex regions. The same edges define particle warnings and body contact. */
final class RatmasterRegion {
    private record Polygon(List<Vec3> vertices) {
        boolean intersects(AABB body) {
            if (separated(body, 1, 0) || separated(body, 0, 1)) return false;
            for (int i = 0; i < vertices.size(); i++) {
                var edge = vertices.get((i + 1) % vertices.size()).subtract(vertices.get(i));
                if (separated(body, -edge.z, edge.x)) return false;
            }
            return true;
        }

        private boolean separated(AABB body, double x, double z) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (var point : vertices) {
                double projection = point.x * x + point.z * z;
                min = Math.min(min, projection);
                max = Math.max(max, projection);
            }
            double center = (body.minX + body.maxX) * .5 * x + (body.minZ + body.maxZ) * .5 * z;
            double radius = (body.maxX - body.minX) * .5 * Math.abs(x) + (body.maxZ - body.minZ) * .5 * Math.abs(z);
            return center + radius <= min || center - radius >= max;
        }
    }

    private final List<Polygon> polygons = new ArrayList<>();
    private final List<List<Vec3>> contours = new ArrayList<>();
    private final double floor, height;

    private RatmasterRegion(double floor, double height) {
        this.floor = floor;
        this.height = height;
    }

    static RatmasterRegion attack(int pattern, int tier, Vec3 center, Vec3 locked, double angle) {
        var region = new RatmasterRegion(center.y, pattern == 0 ? .8 : 2.4);
        var forward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        var across = new Vec3(-forward.z, 0, forward.x);
        switch (pattern) {
            case 0 -> region.strip(locked, across, 16, 16, 1.65);
            case 1 -> region.sector(center, 21, angle - .29, .58);
            case 2 -> {
                for (var lane : lanes(center, angle, tier)) region.strip(lane, forward, 17, 17, 1.15);
            }
            case 3 -> region.sector(center, tier == 2 ? 11 : 8, angle + .55, Math.PI * 2 - 1.1);
            case 4 -> {
                for (var carrier : carriers(center, angle)) region.sector(carrier, 2.7, 0, Math.PI * 2);
            }
            default -> throw new IllegalArgumentException("Unknown Ratmaster attack: " + pattern);
        }
        return region;
    }

    static List<Vec3> lanes(Vec3 center, double angle, int tier) {
        if (tier == 0) return List.of(center);
        var across = new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(4);
        return List.of(center.subtract(across), center, center.add(across));
    }

    static RatmasterRegion ballImpact(int tier, Vec3 center, double angle, int beat) {
        var region = new RatmasterRegion(center.y, 1.6);
        var forward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        for (var lane : lanes(center, angle, tier)) {
            region.strip(ballPosition(lane, forward, beat), forward, 1.15, 1.15, 1.15);
        }
        return region;
    }

    static Vec3 ballPosition(Vec3 lane, Vec3 forward, int beat) {
        double progress = Math.max(0, Math.min(1, (beat - 38) / 40.0));
        return lane.add(forward.scale(-16 + 32 * progress));
    }

    static List<Vec3> carriers(Vec3 center, double angle) {
        var positions = new ArrayList<Vec3>(4);
        for (int i = 0; i < 4; i++) {
            double turn = angle + i * Math.PI / 2;
            positions.add(center.add(Math.cos(turn) * 5, .6, Math.sin(turn) * 5));
        }
        return positions;
    }

    private void strip(Vec3 at, Vec3 along, double before, double after, double halfWidth) {
        var side = new Vec3(-along.z * halfWidth, 0, along.x * halfWidth);
        var start = at.subtract(along.scale(before));
        var end = at.add(along.scale(after));
        var corners = List.of(start.add(side), end.add(side), end.subtract(side), start.subtract(side));
        polygons.add(new Polygon(corners));
        contours.add(corners);
    }

    private void sector(Vec3 at, double radius, double start, double span) {
        int segments = Math.max(2, (int) Math.ceil(span * 5));
        var outline = new ArrayList<Vec3>(segments + 2);
        if (span < Math.PI * 2) outline.add(at);
        Vec3 previous = null;
        for (int i = 0; i <= segments; i++) {
            double angle = start + span * i / segments;
            var point = at.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            if (previous != null) polygons.add(new Polygon(List.of(at, previous, point)));
            outline.add(point);
            previous = point;
        }
        contours.add(outline);
    }

    boolean hits(AABB body) {
        return body.minY < floor + height && body.maxY > floor - .25
                && polygons.stream().anyMatch(polygon -> polygon.intersects(body));
    }

    List<Vec3> warning() {
        var points = new ArrayList<Vec3>();
        for (var contour : contours) {
            for (int i = 0; i < contour.size(); i++) {
                var from = contour.get(i);
                var to = contour.get((i + 1) % contour.size());
                int count = (int) Math.ceil(from.distanceTo(to));
                for (int j = 0; j < count; j++) {
                    var point = from.lerp(to, (double) j / count);
                    points.add(new Vec3(point.x, floor + .12, point.z));
                }
            }
        }
        return points;
    }
}

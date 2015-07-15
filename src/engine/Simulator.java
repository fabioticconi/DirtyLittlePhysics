/**
 * Copyright 2014 Fabio Ticconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package engine;

import java.util.Arrays;

import shapes.Point;
import utils.Vect3D;

/**
 * Entry point for the engine. Simulates the movement of all
 * added particles by taking into consideration the properties of the
 * cell they are into. It uses Verlet Integration for fast and accurate simulation
 * of even velocity-dependent forces (like drag).
 * 
 * <br />
 * <br />
 * 
 * <b>This class is NOT Thread-safe.</b>
 * 
 * @author Fabio Ticconi
 */
public class Simulator
{
    public static final Vect3D nullVector = new Vect3D(0.0, 0.0, 0.0);

    /**
     * Manages all 3D terrain data, allowing the physics to retrieve
     * Cell types.
     */
    public interface Map
    {
        /**
         * Returns true if the point is outside of the Map,
         * false otherwise.
         * 
         * @param p
         * @return
         */
        public boolean isOverBounds(final Point p);

        /**
         * Returns the Cell object including the given point.
         * 
         * @param p
         *            a {@link Point} to get the Cell of
         */
        public Cell getCell(Point p);

        /**
         * Returns true if the two points belong to the same cell.
         * 
         * @param p1
         * @param p2
         * @return
         */
        public boolean areNeighbours(final Point p1, final Point p2);

        /**
         * Returns true if the point can move to the destination,
         * false otherwise.
         * 
         * @param from
         * @param to
         * @return
         */
        public boolean canMoveTo(final Point from, final Point to);
    }

    private final Map world;

    // initial maximumb number of particles, used to
    // initialise the array
    public int        MAX_NUM_OF_PARTICLES = 1000;

    private Vect3D    gravity;

    int               NUM_OF_PARTICLES;
    Particle          particles[];

    public Simulator(final Map world)
    {
        if (world == null)
            throw new IllegalArgumentException("A world must be defined!");

        this.world = world;

        particles = new Particle[MAX_NUM_OF_PARTICLES];
        NUM_OF_PARTICLES = 0;

        setGravity(new Vect3D(0d, 0d, -9.81d));
    }

    /**
     * Sets the gravity as an <b>acceleration</b>,
     * therefore it will be directly added after the
     * dynamic forces have been calculated and summed up
     * together.<br />
     * Example: for Earth, the vector would be (0, 0, -9.81).
     * 
     * @param gravity
     */
    public void setGravity(final Vect3D gravity)
    {
        this.gravity = gravity;
    }

    /**
     * Adds a new particle to the simulator.<br />
     * O(1)
     * 
     * @param p
     */
    public void addParticle(final Particle p)
    {
        if (p == null)
            return;

        if (NUM_OF_PARTICLES >= MAX_NUM_OF_PARTICLES)
        {
            MAX_NUM_OF_PARTICLES *= 2;

            // TODO: check heap exception
            particles = Arrays.copyOf(particles, MAX_NUM_OF_PARTICLES);
        }

        particles[NUM_OF_PARTICLES++] = p;
    }

    /**
     * Removes a particle from the simulator,
     * if it was there. <br />
     * O(N)
     * 
     * @param p
     */
    public void removeParticle(final Particle p)
    {
        if (p == null)
            return;

        for (int i = 0; i < NUM_OF_PARTICLES; i++)
            if (particles[i] == p)
            {
                particles[i] = particles[--NUM_OF_PARTICLES];
                break;
            }
    }

    /**
     * <p>
     * Verlet Velocity integration method, slightly modified to take into account forces dependent on velocity (eg,
     * fluid dragCoefficient). Reference (particle physics paper):
     * http://pages.csam.montclair.edu/~yecko/ferro/papers/FerroDPD/GrootWarren_ReviewDPD_97.pdf
     * </p>
     * 
     * <p>
     * However, as also described here: http://gamedev.stackexchange.com/a/41917/51181 we used a lambda of 1 and
     * re-arranged a bit to reduce the number of divisions and multiplications. Semantically is the same as the above
     * paper.
     * </p>
     * 
     * <p>
     * <b>Note: the stackexchange answer is actually wrong in the last part, where: <br />
     * velocity += timestep * (newAcceleration - acceleration) / 2; <br />
     * should be: <br />
     * velocity += timestep * (acceleration - newAcceleration) / 2;</b>
     * </p>
     * 
     * <p>
     * Note2: if particles will begin interacting with each other (attractors for example), this will need some
     * modifications (updating all positions before recalculating new accelerations?)
     * </p>
     * 
     * @param dt
     *            how much to advance the simulation of
     */
    public void update(final double dt)
    {
        final Vect3D netGravity = new Vect3D();
        final Vect3D acc = new Vect3D();
        double buoyancy;

        // halve the delta t, to save
        // a few divisions
        final double dt2 = dt / 2.0d;

        final Vect3D newpos = new Vect3D();

        Particle p;
        Vect3D force;
        Vect3D vel;
        Vect3D oldpos;
        Cell cell;
        for (int i = 0; i < NUM_OF_PARTICLES; i++)
        {
            p = particles[i];

            // oldpos is a reference to the actual
            // particle center
            oldpos = p.center;
            // newpos is a temporary vector, we copy
            // the current position inside
            // and perform the updates with it
            newpos.assign(oldpos);

            vel = p.vel;

            cell = world.getCell(p.center);

            buoyancy = cell.getBuoyancy(p);

            // Gravity must be corrected by the buoyancy.
            // Note: normally this would only make sense for the "z" dimension,
            // but who are we to limit your creativity?
            netGravity.x = gravity.x * buoyancy;
            netGravity.y = gravity.y * buoyancy;
            netGravity.z = gravity.z * buoyancy;

            force = cell.getForces(p);

            // as the Gravitational force is equal to m*g,
            // we can add it after the cumulative force has been
            // converted to acceleration - in this way, we store it
            // as "g", independent of mass, and then add it to the
            // newly found acceleration.
            acc.x = (force.x / p.mass) + netGravity.x;
            acc.y = (force.y / p.mass) + netGravity.y;
            acc.z = (force.z / p.mass) + netGravity.z;

            newpos.x += dt * (vel.x + (dt2 * acc.x));
            newpos.y += dt * (vel.y + (dt2 * acc.y));
            newpos.z += dt * (vel.z + (dt2 * acc.z));

            // we may, or may not, be in another cell
            // right now
            cell = world.getCell(newpos);

            // if the new position rests in the same
            // cell of the old position, we can update
            // the particle speed and conclude the step
            if (world.canMoveTo(oldpos, newpos))
            {
                // we assign the values of newpos
                // inside the actual particle center
                oldpos.assign(newpos);

                vel.x += dt * acc.x;
                vel.y += dt * acc.y;
                vel.z += dt * acc.z;

                force = cell.getForces(p);

                acc.x -= (force.x / p.mass) + netGravity.x;
                acc.y -= (force.y / p.mass) + netGravity.y;
                acc.z -= (force.z / p.mass) + netGravity.z;

                vel.x += dt2 * acc.x;
                vel.y += dt2 * acc.y;
                vel.z += dt2 * acc.z;
            }
            else
                // if they don't, we should move the particle
                // only up to the border between the cells
                // this also applies to the case of being
                // at the edge of the world.
                // FIXME: for now we don't care an use a
                // bad heuristics: the velocity is halved and
                // the problem is postponed to the next time step
                vel.div(2.0);

            // FIXME: kept only for debug,
            // soon the acceleration should be removed from the Particle class
            p.acc.assign(acc);
        }
    }
}

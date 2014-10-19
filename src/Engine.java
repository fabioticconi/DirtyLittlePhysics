import java.util.Arrays;


public class Engine
{
    // Fluid viscosities likely to be used
    public final static double AIR_VISCOSITY   = 0.00001983d;
    public final static double WATER_VISCOSITY = 0.001d;
    
    // Common densities
    public final static double AIR_DENSITY   = 1.2d; // pretty dense air
    public final static double WATER_DENSITY = 1000d;
    
    // initial maximumb number of particles, used to
    // initialise the array
    public int MAX_NUM_OF_PARTICLES = 1000;
    
    Vect3D gravity;
    double dragCoefficient;
    double fluidDensity;
    
    int NUM_OF_PARTICLES;
    Particle particles[];
    
    public Engine()
    {
        particles = new Particle[MAX_NUM_OF_PARTICLES];
        NUM_OF_PARTICLES = 0;
        
        setGravity(new Vect3D(0d, 0d, -9.81d));
        setDragCoefficient(AIR_VISCOSITY);
        setFluidDensity(AIR_DENSITY);
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
    public void setGravity(Vect3D gravity)
    {
        this.gravity = gravity;
    }
    
    /**
     * Using <a href="http://en.wikipedia.org/wiki/Stokes%27_law">Stoke's Law</a>,
     * sets the fluid viscosity that will later be put in the
     * calculation of the linear drag.
     * 
     * @param fluidViscosity the viscosity of the fluid the particles
     * are swimming in, in Pa*s.
     */
    public void setDragCoefficient(double fluidViscosity)
    {
        this.dragCoefficient = Math.pow(6.0 * Math.PI, 2.0)*fluidViscosity;
    }
    
    /**
     * The density of the fluid the particles swim into.
     * It's used for the buoyancy, ie the reduction in
     * gravitational force when falling into a fluid.
     * 
     * @param fluidDensity in kg/m^3
     */
    public void setFluidDensity(double fluidDensity)
    {
        this.fluidDensity = fluidDensity;
    }
    
    /**
     * Adds a new particle to the engine.
     * O(1)
     * @param p
     */
    public void addParticle(Particle p)
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
     * Removes a particle from the engine,
     * if it was there.
     * O(N)
     * @param p
     */
    public void removeParticle(Particle p)
    {
        if (p == null)
            return;
        
        for (int i = 0; i < NUM_OF_PARTICLES; i++)
        {
            if (particles[i] == p)
            {
                particles[i] = particles[--NUM_OF_PARTICLES];
                break;
            }
        }
    }
    
    private Vect3D getCumulativeForce(Particle p)
    {
        Vect3D force = new Vect3D();
        
        double v3x = p.vel.x * p.vel.x * p.vel.x;
        double v3y = p.vel.y * p.vel.y * p.vel.y;
        double v3z = p.vel.z * p.vel.z * p.vel.z;
     
        // Experimentally found mixture of Stoke's Law and
        // classic quadric drag. Yields meaningful terminal velocities
        // using "real values". The crucial bit here is the
        // cubed velocity, as Stoke's Law alone (so called linear drag)
        // gives crazyly high values for any particle bigger than a finger nail.
        // The real drag function uses squared velocity and the "drag coefficient"
        // that, unlike our constant here, is dependent on the Reynolds number.
        // Guess what? The reynolds number is proportional to velocity to.
        // Hence the cubed velocity. Feel free to play with it but make sure
        // you double check with real values and a good terminal velocity calculator.
        force.x = - dragCoefficient * p.radius * v3x;
        force.y = - dragCoefficient * p.radius * v3y;
        force.z = - dragCoefficient * p.radius * v3z;
                
        return force;
    }
    
    /**
     * Verlet Velocity integration method,
     * slightly modified to take into account
     * forces dependent on velocity (eg, fluid dragCoefficient).
     * Reference (particle physics paper):
     * http://pages.csam.montclair.edu/~yecko/ferro/papers/FerroDPD/GrootWarren_ReviewDPD_97.pdf
     * 
     * <br /><br />
     * 
     * However, as also described here:
     * http://gamedev.stackexchange.com/a/41917/51181
     * we used a lambda of 1 and re-arranged a bit to
     * reduce the number of divisions and multiplications.
     * Semantically is the same as the above paper.
     * 
     * <br /><br />
     * 
     * <b>Note: the stackexchange answer is actually wrong in the last
     * part, where: <br />
     * velocity += timestep * (newAcceleration - acceleration) / 2;
     * <br />should be: <br />
     * velocity += timestep * (acceleration - newAcceleration) / 2;</b><br /><br />
     * 
     * <br /><br />
     * 
     * Note2: if particles will begin interacting with
     * each other (attractors for example), this will
     * need some modifications (updating all positions before
     * recalculating new accelerations?)
     * 
     * @param dt how much to advance the simulation of
     */
    public void update(double dt)
    {
        Vect3D netGravity = new Vect3D();
        Vect3D acc = new Vect3D();
        
        // halve the delta t, to save
        // a few divisions
        double dt2 = dt / 2d;
        
        Particle p;
        Vect3D force;
        Vect3D vel;
        Vect3D pos;
        for (int i = 0; i < NUM_OF_PARTICLES; i++)
        {
            p = particles[i];
            pos = p.pos;
            vel = p.vel;
            
            // Gravity must be corrected by the buoyancy.
            // Reference: http://lorien.ncl.ac.uk/ming/particle/cpe124p2.html
            // Note: normally this would only make sense for the "z" dimension,
            // but who are we to limit your creativity?
            netGravity.x = gravity.x * ((p.density - fluidDensity) / p.density);
            netGravity.y = gravity.y * ((p.density - fluidDensity) / p.density);
            netGravity.z = gravity.z * ((p.density - fluidDensity) / p.density);
            
            force = getCumulativeForce(p);
            
            acc.x = (force.x / p.mass) + netGravity.x;
            acc.y = (force.y / p.mass) + netGravity.y;
            acc.z = (force.z / p.mass) + netGravity.z;
                        
            pos.x += dt * (vel.x + (dt2 * acc.x));
            pos.y += dt * (vel.y + (dt2 * acc.y));
            pos.z += dt * (vel.z + (dt2 * acc.z));
            
            vel.x += dt * acc.x;
            vel.y += dt * acc.y;
            vel.z += dt * acc.z;
                        
            force = getCumulativeForce(p);
            
            acc.x -= (force.x / p.mass) + netGravity.x;
            acc.y -= (force.y / p.mass) + netGravity.y;
            acc.z -= (force.z / p.mass) + netGravity.z;
            
            vel.x += dt2 * acc.x;
            vel.y += dt2 * acc.y;
            vel.z += dt2 * acc.z;
            
            // FIXME: kept only for debug,
            // soon the acceleration should be removed from the Particle class
            p.acc.x = acc.x;
            p.acc.y = acc.y;
            p.acc.z = acc.z;
        }
    }
    
    public static void main(String[] args)
    {
        final double STEP = 0.01d;
        
        Engine w = new Engine();
       
        Particle p = new Particle();
        p.setMass(70d);
        p.setRadius(0.2505887894d);
        System.out.println("Mass: " + p.mass + ", Radius: " + p.radius + ", Density: " + p.density);
        p.vel.x = 1d;
        p.vel.y = -1d;
        p.vel.z = 0d;
        System.out.println("First particle: initial values");
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
        w.addParticle(p);
        w.update(STEP);
        System.out.println("After 1 step (" + STEP + " seconds):");
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
        w.update(STEP);
        System.out.println("After 1 step (" + STEP + " seconds):");
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
        w.update(STEP);
        System.out.println("After 1 step (" + STEP + " seconds):");
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
        
        long time1 = System.currentTimeMillis();
        System.out.print("Adding particles.. ");
        for (int i = 0; i < 200000; i++)
        {
            Particle p2 = new Particle();
            p2.vel.x = 1d;
            w.addParticle(p2);
        }
        long time2 = System.currentTimeMillis();
        System.out.println((time2 - time1) + " ms");
        
        time1 = System.currentTimeMillis();
        System.out.print("Moving forward the simulation.. ");
        for (double t = 0d; t < 1d; t = t + STEP)
        {
            w.update(STEP);
        }
        time2 = System.currentTimeMillis();
        System.out.println((time2 - time1) + " ms");
        
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
        
        time1 = System.currentTimeMillis();
        System.out.print("Moving forward the simulation.. ");
        for (double t = 0d; t < 10d; t = t + STEP)
        {
            w.update(STEP);
        }
        time2 = System.currentTimeMillis();
        System.out.println((time2 - time1) + " ms");
        
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
        
        time1 = System.currentTimeMillis();
        System.out.print("Moving forward the simulation.. ");
        for (double t = 0d; t < 10d; t = t + STEP)
        {
            w.update(STEP);
        }
        time2 = System.currentTimeMillis();
        System.out.println((time2 - time1) + " ms");
        
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
        
        time1 = System.currentTimeMillis();
        System.out.print("Moving forward the simulation.. ");
        for (double t = 0d; t < 10d; t = t + STEP)
        {
            w.update(STEP);
        }
        time2 = System.currentTimeMillis();
        System.out.println((time2 - time1) + " ms");
        
        System.out.println("Pos: " + p.pos.x + " " + p.pos.y + " " + p.pos.z);
        System.out.println("Vel: " + p.vel.x + " " + p.vel.y + " " + p.vel.z);
        System.out.println("Acc: " + p.acc.x + " " + p.acc.y + " " + p.acc.z);
    }
}

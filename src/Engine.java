
public class Engine
{
    // Fluid viscosities likely to be used
    public final static double AIR_VISCOSITY = 0.01983d;
    public final static double WATER_VISCOSITY = 0.001d;
    
    public final static int MAX_NUM_OF_PARTICLES = 200000;
    
    Vect3D gravity;
    double dragCoefficient;
    
    int NUM_OF_PARTICLES;
    Particle particles[];
    
    public Engine()
    {
        particles = new Particle[MAX_NUM_OF_PARTICLES];
        NUM_OF_PARTICLES = 0;
        
        setGravity(new Vect3D(0d, 0d, -9.81d));
        setDragCoefficient(AIR_VISCOSITY);
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
        this.dragCoefficient = 6*Math.PI*fluidViscosity;
    }
    
    /**
     * Adds a new particle to the engine.
     * O(1)
     * @param p
     */
    public void addParticle(Particle p)
    {
        if (NUM_OF_PARTICLES > MAX_NUM_OF_PARTICLES || p == null)
            return;
                
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
     
        // Stoke's Law (fluid drag force)
        force.x = - dragCoefficient*p.radius*p.vel.x;
        force.y = - dragCoefficient*p.radius*p.vel.y;
        force.z = - dragCoefficient*p.radius*p.vel.z;
        
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
        // half the delta t, to save
        // a few divisions
        double dt2 = dt / 2d;
        
        Particle p;
        Vect3D force;
        Vect3D acc;
        Vect3D vel;
        Vect3D pos;
        for (int i = 0; i < NUM_OF_PARTICLES; i++)
        {
            p = particles[i];
            pos = p.pos;
            vel = p.vel;
            acc = p.acc;
            
            force = getCumulativeForce(p);
            
            acc.x = (force.x / p.mass) + gravity.x;
            acc.y = (force.y / p.mass) + gravity.y;
            acc.z = (force.z / p.mass) + gravity.z;
            
            pos.x += dt * (vel.x + (dt2 * acc.x));
            pos.y += dt * (vel.y + (dt2 * acc.y));
            pos.z += dt * (vel.z + (dt2 * acc.z));
            
            vel.x += dt * acc.x;
            vel.y += dt * acc.y;
            vel.z += dt * acc.z;
            
            force = getCumulativeForce(p);
            
            acc.x = -((force.x / p.mass) + gravity.x) + acc.x;
            acc.y = -((force.y / p.mass) + gravity.y) + acc.y;
            acc.z = -((force.z / p.mass) + gravity.z) + acc.z;
            
            vel.x += dt2 * acc.x;
            vel.y += dt2 * acc.y;
            vel.z += dt2 * acc.z;
        }
    }
    
    public static void main(String[] args)
    {
        final double STEP = 0.01d;
        
        Engine w = new Engine();
        
        Particle p = new Particle();
        p.vel.x = 1d;
        p.vel.y = 0d;
        p.vel.z = -1d;
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
        
        long time1 = System.currentTimeMillis();
        System.out.print("Adding particles.. ");
        for (int i = 0; i < 100000; i++)
        {
            Particle p2 = new Particle();
            p2.vel.x = 1d;
            w.addParticle(p2);
        }
        long time2 = System.currentTimeMillis();
        System.out.println((time2 - time1) + " ms");
        
        time1 = System.currentTimeMillis();
        System.out.print("Moving forward the simulation.. ");
        for (double t = 0d; t < 100d; t = t + STEP)
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
        for (double t = 0d; t < 100d; t = t + STEP)
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
        for (double t = 0d; t < 100d; t = t + STEP)
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
        for (double t = 0d; t < 1000d; t = t + STEP)
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

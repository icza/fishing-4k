import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Color;
import java.awt.Event;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Vector;

@SuppressWarnings("serial")
public class F extends Applet implements Runnable {
	
	public void start() {
		new Thread( this ).start();
	}
	
	public void run() {
		final AudioClip catchClip  = newAudioClip( getClass().getResource( "c" ) );
		final AudioClip sonicClip  = newAudioClip( getClass().getResource( "s" ) );
		final AudioClip unloadClip = newAudioClip( getClass().getResource( "u" ) );
		// ==============================================================================
		// GAME WIDE CONSTANTS
		// ==============================================================================
		
		final int   SCREEN_WIDTH          = 800;
		final int   SCREEN_HEIGHT         = 600;
		final int   NET_SIZE              =  36;
		final int   BOAT_HEIGHT           =  40;
		final int   FISH_WIDTH            =  26;
		final int   FISH_HEIGHT           =  10;
		final int   BUBBLE_SIZE           =   5;
		final int   SEA_LEVEL             = 100;    // Without waves
		final float SURFACE_OMEGA         = 0.033f; // Omega of the sin wave of the surface.
		final int   MAX_SURFACE_AMPLITUDE =  44;
		final int   NET_CAPACITY          =  20;
		final int   SONIC_CHARGE_FULL     = 280;
		final int   BOAT_VX               =   6;
		final int   BOAT_VY               =  10;
		
		
		// INIT THE POLYGONS
		// Polygon of the boat.
		final Polygon BOAT_POLYGON       = new Polygon( new int[] { -40, -27, 26, 40, 13, 0, -14 }, new int[] { 0, 20, 20, 0, 0, -20, 0 }, 7 );
		// Polygon of the fish heading right.
		final Polygon FISH_POLYGON_RIGHT = new Polygon( new int[] { 13, 10, 7, 1, -3, -7, -13, -13, -7, -3, 1, 6, 10, 13 }, new int[] { -2, -4, -5, -5, -3, -2, -5, 5, 1, 3, 5, 5, 3, 1 }, 14 );
		// The polygon of fish heading left has the same coordinates just the X's multiplied by -1
		final Polygon FISH_POLYGON_LEFT  = new Polygon();
		int i;
		for ( i = 0; i < 14; i++ )
			FISH_POLYGON_LEFT.addPoint( -FISH_POLYGON_RIGHT.xpoints[ i ], FISH_POLYGON_RIGHT.ypoints[ i ] );
		
		
		// ==============================================================================
		// GAME MODEL AND STATE (MODEL OF MVC)
		// ==============================================================================
		
		// Moving object has 4 properties: x, y, vx, vy
		// The water surface of the 'sea'.
		float surfacePhase = 0, surfaceAmplitude = 0;
		// The boat (boat x coordinate, and net y coordinate
		float boat0f = 0, boat1f = 0;
		// Vector of the fishes.
		Vector< float[] > fishes           = null; // Fishes have 4 properties: x, y, vx, vy
		// Vector of the bubbles.
		Vector< float[] > bubbles          = null; // Bubbles have 5 properties: x, y, vx, vy, oscillationPhase
		// Number of fishes the player caught. 
		int               fishesCaught     = 0;
		// Number of fishes the player caught. 
		int               fishesMissed     = 10; // The value of 10 will trigger a new game
		// Iteration counter, tells which iteration are we in.
		int               iterationCounter = -1; // To initiate the initial init
		// How many fish are in the next
		int               fishesInNet      = 0;
		// Sonic weapon charge state
		int               sonicCharge      = SONIC_CHARGE_FULL;
		// Radius of the sonic weapon's wave
		int               sonicWaveR       = -1;
		// Center point of the sonic wave
		int               sonicWaveCx      = 0, sonicWaveCy = 0;
		// Tells if net unloading is in progress.
		boolean           unloading        = false;
		
		
		// ==============================================================================
		// GAME VARIABLES
		// ==============================================================================
		
		// Set up the graphics stuff, double-buffering.
		final BufferedImage buffer   = new BufferedImage( SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB );
		final Graphics2D    graphics = (Graphics2D) buffer.getGraphics();
		
		// Uncomment to turn on antialiasing
		//( (Graphics2D) graphics ).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		
		long lastTime = 0;
		
		int boatX = 0;
		int boat1 = 0;
		int boatY = 0;
		int netY  = 0;
		while ( true ) {
			if ( k[ 5 ] && System.nanoTime() - lastTime > 50000000 || iterationCounter < 0 ) { // 20 fps
				// ==============================================================================
				// GAME CONTROLLER AND LOGIC (CONTROLLER OF MVC)
				// ==============================================================================
				
				if ( fishesMissed >= 10 ) {
					// Init new game
					surfacePhase = 0; surfaceAmplitude = 2;
					boat0f = SCREEN_WIDTH / 2; boat1f = ( NET_SIZE + BOAT_HEIGHT ) / 2;
					fishes           = new Vector< float[] >();
					bubbles          = new Vector< float[] >();
					fishesCaught     = 0;
					fishesMissed     = 0;
					iterationCounter = 0;
					sonicCharge      = SONIC_CHARGE_FULL;
					fishesInNet      = 0;
					sonicWaveR       = -1;
					unloading        = false;
					boatX = (int) boat0f;
        			boat1 = (int) boat1f;
        			boatY = SEA_LEVEL + (int) ( surfaceAmplitude * Math.sin( surfacePhase + SURFACE_OMEGA * boat0f ) );
        			netY  = boatY + boat1;
				}
				else {
					// Update
					lastTime = System.nanoTime();
					
					// CALCULATE THE NEXT ITERATION
					
					// Move the boat
					// Simulate water drifting
					boat0f -= Math.min( surfaceAmplitude / 10, 3.5f ); // Substraction because waves go from right to left
					// Now we handle the the control keys
					if ( k[ 0 ] ) // LEFT
						boat0f -= BOAT_VX;
					if ( k[ 1 ] ) // RIGHT
						boat0f += BOAT_VX;
					if ( k[ 2 ] ) // UP
						boat1f -= BOAT_VY;
					if ( k[ 3 ] ) // DOWN
						boat1f += BOAT_VY;
					// We check the positions whether they are outside the valid domains
					if ( boat0f < 0 )
						boat0f = 0;
					if ( boat0f > SCREEN_WIDTH - 1 )
						boat0f = SCREEN_WIDTH - 1;
					if ( boat1f < ( NET_SIZE + BOAT_HEIGHT ) / 2 )
						boat1f = ( NET_SIZE + BOAT_HEIGHT ) / 2;
					if ( boat1f > SCREEN_HEIGHT - SEA_LEVEL - 1 )
						boat1f = SCREEN_HEIGHT - SEA_LEVEL - 1;
					
					// Calculate/convert some frequently used values
					boatX = (int) boat0f;
        			boat1 = (int) boat1f;
        			boatY = SEA_LEVEL + (int) ( surfaceAmplitude * Math.sin( surfacePhase + SURFACE_OMEGA * boat0f ) );
        			netY  = boatY + boat1;
        			
					// Charge the sonic weapon
					if ( sonicCharge < SONIC_CHARGE_FULL )
						sonicCharge++;
					if ( k[ 4 ] && sonicCharge == SONIC_CHARGE_FULL ) { // ENTER
						sonicCharge = 0;
						sonicWaveR  = 1;
						sonicWaveCx = boatX;
						sonicWaveCy = boatY;
						sonicClip.loop();
					}
					
					// Unload the net if there are fishes in the net
					if ( boat1 <= ( NET_SIZE + BOAT_HEIGHT ) / 2 + BOAT_VY && fishesInNet > 0 ) {
						if ( !unloading ) {
							unloading = true;
							unloadClip.loop();
						}
						fishesInNet--;
					}
					else
						if ( unloading ) {
							unloading = false;
							unloadClip.stop();
						}
					
					// Increment the water phase:
					surfacePhase += 0.12f;
					// Increment the surface amplitude:
					if ( surfaceAmplitude < MAX_SURFACE_AMPLITUDE )
						surfaceAmplitude += 0.008f;
					
					// Step the sonic wave
					if ( sonicWaveR > 0 && ( sonicWaveR += 40 ) > SCREEN_WIDTH ) {
						sonicWaveR = -1;
						sonicClip.stop();
					}
					
					// Now we check and step the fishes
					for ( i = fishes.size() - 1; i >= 0; i-- ) {
						final float[] fish = fishes.get( i );
						// Check if the fish is in the scope of the sonic wave
						if ( sonicWaveR > 0 )
							if ( (int) Math.pow( fish[ 0 ]                                           - sonicWaveCx, 2 ) + (int) Math.pow( fish[ 1 ] - sonicWaveCy, 2 ) < sonicWaveR*sonicWaveR ||
								 (int) Math.pow( fish[ 0 ] + (fish[ 2 ]>0?+FISH_HEIGHT:-FISH_HEIGHT) - sonicWaveCx, 2 ) + (int) Math.pow( fish[ 1 ] - sonicWaveCy, 2 ) < sonicWaveR*sonicWaveR )
								if ( fish[ 2 ] > 2 || fish[ 2 ] < -2 ) { // Not yet effected by the sonic wave
									fish[ 2 ] /= 4;
									fish[ 3 ] = 2;
								}
						
						// Step the fish
						fish[ 0 ] += fish[ 2 ];
						fish[ 1 ] += fish[ 3 ];
						
						// Did the fish just leave the scene? 
						if ( fish[ 0 ] < -FISH_WIDTH/2 || fish[ 0 ] > SCREEN_WIDTH - 1 + FISH_WIDTH/2 || fish[ 1 ] >= SCREEN_HEIGHT ) {
							fishes.remove( i );
							if ( fish[ 1 ] < SCREEN_HEIGHT )
								fishesMissed++;
						} else if ( fishesInNet < NET_CAPACITY )
						if ( (int) Math.pow( fish[ 0 ]                                           - boatX, 2 ) + (int) Math.pow( fish[ 1 ] - netY, 2 ) < NET_SIZE/2*NET_SIZE/2 ||
							 (int) Math.pow( fish[ 0 ] + (fish[ 2 ]>0?+FISH_HEIGHT:-FISH_HEIGHT) - boatX, 2 ) + (int) Math.pow( fish[ 1 ] - netY, 2 ) < NET_SIZE/2*NET_SIZE/2 ) {
							// Is the fish being caught?
							// Fish is caught, if the center point of the fish is inside the net (which is a circle)
							//                 or if "it would swim into our net"
							fishes.remove( i );
							fishesCaught++;
							fishesInNet ++;
							catchClip.play();
						}
					}
					
					// Now we check and step the bubbles
					for ( i = bubbles.size() - 1; i >= 0; i-- ) {
						final float[] bubble = bubbles.get( i ); 
						// Step the fish
						bubble[ 0 ] += bubble[ 2 ];
						bubble[ 1 ] += bubble[ 3 ];
						bubble[ 4 ] += 0.15f;
						// Did the bubble come out of the water?
						if ( bubble[ 1 ] - BUBBLE_SIZE/2 < SEA_LEVEL + surfaceAmplitude * Math.sin( surfacePhase + SURFACE_OMEGA * bubble[ 0 ] ) )
							bubbles.remove( i );
					}		
					
					// We may "launch" a new fish. As the time goes, probability of launching fish goes higher.
					if ( Math.random() < Math.min( 0.075f, 0.025f + iterationCounter/20000f ) ) {
						// Generate a new fish
						// By FREE I mean the fish can swim there, for example, the fish cannot swim in the air or in the waves.
						final int FREE_WATER_RANGE = SCREEN_HEIGHT - SEA_LEVEL - MAX_SURFACE_AMPLITUDE - FISH_HEIGHT;
						final int MIN_FREE_LEVEL   = SEA_LEVEL + MAX_SURFACE_AMPLITUDE;
						
						final boolean comingFromLeft = Math.random() * 2 > 1;  // 50% chance for coming from left, 50% for right
						final float   startXPos = comingFromLeft ? -FISH_WIDTH/2 : SCREEN_WIDTH - 1 + FISH_WIDTH/2;
						final float   startYPos = MIN_FREE_LEVEL + (float)Math.random() * FREE_WATER_RANGE;
						// We generate an endYPos for determining vy. We want the new fish to head to this point
						final float   endYPos   = MIN_FREE_LEVEL + (float)Math.random() * FREE_WATER_RANGE;
						
						// For vx: 2.2 at the beginning, and maximum value increases 1/20 in every seconds
						// And if it comes from right, it must be negative
						final float   vx        = ( comingFromLeft ? 1 :-1 ) * ( 2.4f + (float)Math.random() * ( iterationCounter / 400f ) );
						// v=s/t where s=endYPos-startYPos and t=SCENE_WIDTH/vx.     vy must be this, if we want the fish to head toward endYPos
						final float   vy        = ( endYPos - startYPos ) / ( (SCREEN_WIDTH+FISH_WIDTH) / Math.abs( vx ) );
						
						// We now have all parameter for a new fish
						fishes.add( new float[] { startXPos, startYPos, vx, vy } );
					}

					// A new bubble may appear in the water
					if ( Math.random() < 0.03f )
						bubbles.add( new float[] { (int) ( Math.random() * SCREEN_WIDTH ), SCREEN_HEIGHT, 0, -1.6f, 0 } );
					
					iterationCounter++;
					
					// If game over:
					if ( fishesMissed >= 10 )
						k[ 5 ] = false; // Pause the game
				}
			}
			if ( !k[ 5 ] ) { // Stop looped sounds if game is paused
				if ( sonicWaveR > 0 )
					sonicClip.stop();
				if ( unloading ) {
					unloading = false;
					unloadClip.stop();
				}
			}
			
			// ==============================================================================
			// GAME VIEW (VIEW OF MVC)
			// ==============================================================================
			
			// Draw the water
			graphics.setPaint( new GradientPaint( 0, SEA_LEVEL - MAX_SURFACE_AMPLITUDE, new Color( 8, 74, 174 ), 0, SCREEN_HEIGHT-1, new Color( 2, 19, 44 ) ) );
			graphics.fillRect( 0, SEA_LEVEL - MAX_SURFACE_AMPLITUDE, SCREEN_WIDTH, SCREEN_HEIGHT - SEA_LEVEL + MAX_SURFACE_AMPLITUDE );
			// Draw the sonic wave
			if ( sonicWaveR > 0 )
				for ( i = 0; i < 25; i++ ) {
					graphics.setColor( new Color( i * 10, i * 10, i * 10, i * 10 ) );
					final int r = sonicWaveR + i - 25;
					graphics.drawOval( sonicWaveCx - r, sonicWaveCy - r, r<<1, r<<1 );
				}
			// Draw the open air
			for ( i = 0; i < SCREEN_WIDTH; i++ ) {
				// Nice gradient fill for the AIR from left to right
				// Note: using of java.awt.GradientPaint for this resulted in much higher cpu loading!!!
				final int MIN_RED     = 187;
				final int MIN_GREEN   =   7;
				final int MIN_BLUE    =   7;
				final int DELTA_RED   = 251 - MIN_RED  ;
				final int DELTA_GREEN = 185 - MIN_GREEN;
				final int DELTA_BLUE  =  53 - MIN_BLUE ;
				
				graphics.setColor( new Color( MIN_RED + DELTA_RED*i/SCREEN_WIDTH, MIN_GREEN + DELTA_GREEN*i/SCREEN_WIDTH, MIN_BLUE + DELTA_BLUE*i/SCREEN_WIDTH ) );
				graphics.drawLine( i, 0, i, SEA_LEVEL + (int) ( surfaceAmplitude * Math.sin( surfacePhase + SURFACE_OMEGA * i ) ) );
			}
			
			// Draw decorations of the game (bubbles only for now).
			graphics.setColor( new Color( 160, 160, 255 ) ); // Bubble color
			for ( i = bubbles.size() - 1; i >= 0; i-- ) {
				final float[] bubble = bubbles.get( i );
				graphics.drawOval( (int) (bubble[ 0 ] + 5*Math.sin( bubble[ 4 ] )- BUBBLE_SIZE/2), (int) (bubble[ 1 ] - BUBBLE_SIZE/2), BUBBLE_SIZE, BUBBLE_SIZE );
			}
			
			// Draw the fishes
			graphics.setColor( new Color( 202, 201, 212 ) ); // Fish color
			for ( i = fishes.size() - 1; i >= 0; i-- ) {
				final float[] fish = fishes.get( i );
				final Polygon fishPolygon = fish[ 2 ] > 0 ? FISH_POLYGON_RIGHT : FISH_POLYGON_LEFT;
				
				graphics.translate( (int) fish[ 0 ], (int) fish[ 1 ] );
				graphics.fillPolygon( fishPolygon );
				graphics.translate( -(int) fish[ 0 ], -(int) fish[ 1 ] );
			}
			
			// Draw the boat
			// First we draw the net
			// The net consists of the rope which connects it to the boat,
			// and a circle with 3 horizontal and 3 vertical lines 
			final int CHORD_LENGTH = (int) ( NET_SIZE * 0.433f );   // This is the length of the half of the chord which is parallel with the diameter of the circle, and is at he half of the radius. 0.433=sqrt(3)/2/2
			
			// Fill the net based on how many fishes it contains
			// Current color is fish color
			graphics.setClip( 0, (int) ( netY +(NET_CAPACITY/2 - fishesInNet) * (NET_CAPACITY/2 - fishesInNet) * 4f / (NET_CAPACITY*NET_CAPACITY) * (NET_CAPACITY/2 < fishesInNet ? -NET_SIZE/2 : NET_SIZE/2) ), SCREEN_WIDTH, SCREEN_HEIGHT );
			graphics.fillOval( boatX - NET_SIZE/2, netY - NET_SIZE/2, NET_SIZE, NET_SIZE );
			graphics.setClip( null );
			graphics.setColor( new Color( 202, 201, 112 ) ); // Net color
			// The outline of the net
			graphics.drawOval( boatX - NET_SIZE/2, netY - NET_SIZE/2, NET_SIZE, NET_SIZE );
			// The rope of the net
			graphics.drawLine( boatX             , boatY, boatX, netY - NET_SIZE );
			graphics.drawLine( boatX - NET_SIZE/2, netY , boatX, netY - NET_SIZE );
			graphics.drawLine( boatX + NET_SIZE/2, netY , boatX, netY - NET_SIZE );
			// The vertical lines of net
			graphics.drawLine( boatX - NET_SIZE/4, netY - CHORD_LENGTH, boatX - NET_SIZE/4, netY + CHORD_LENGTH );
			graphics.drawLine( boatX             , netY - NET_SIZE/2  , boatX             , netY + NET_SIZE/2   );
			graphics.drawLine( boatX + NET_SIZE/4, netY - CHORD_LENGTH, boatX + NET_SIZE/4, netY + CHORD_LENGTH );
			// The horizontal lines of net
			graphics.drawLine( boatX - CHORD_LENGTH, netY - NET_SIZE/4, boatX + CHORD_LENGTH, netY - NET_SIZE/4 );
			graphics.drawLine( boatX - NET_SIZE/2  , netY             , boatX + NET_SIZE/2  , netY              );
			graphics.drawLine( boatX - CHORD_LENGTH, netY + NET_SIZE/4, boatX + CHORD_LENGTH, netY + NET_SIZE/4 );
			
			// ...and the boat
			final AffineTransform storedAffineTransform = graphics.getTransform(); // We store the transform, because we draw the boat rotated.
			
			/* Surface following degree of the boat (for example 0.7 means 70%: if surface 
			   deviates 100 degrees from the horizontal, the boat will deviate 70 degrees). */
			graphics.rotate( Math.atan( SURFACE_OMEGA * surfaceAmplitude * Math.cos( surfacePhase + SURFACE_OMEGA * boat0f ) ) * 0.7f, boat0f, boatY );
			// Degree of sinking of the boat to be in rest (for example 0.15 means 15%).
			graphics.translate( boatX, boatY - (int) ( BOAT_HEIGHT * (0.5f-0.15f) ) );
			graphics.setColor( new Color( 115, 228, 117 ) ); // Boat color
			graphics.fillPolygon( BOAT_POLYGON );
			graphics.setTransform( storedAffineTransform );
			
			// Draws the texts must be displayed on the scene.
			graphics.setColor( new Color( 255, 255, 255 ) ); // Text color
			
			graphics.drawString( "Caught: ".concat( String.valueOf( fishesCaught ) ), 5, 14 );
			graphics.drawString( "Missed: ".concat( String.valueOf( fishesMissed ) ), SCREEN_WIDTH - 67, 14 );
			
			if ( !k[ 5 ] )
				// Putting 3 texts centering to the scene
				for ( i = 0; i < 3; i++ ) {
					// Arrow chars: \u2190\u2192\u2191\u2193
					final String text = i == 0 ? ( fishesMissed >= 10 ? "Game over!" : "Game paused" ) : i == 1 ? "SPACE - pause/resume, ARROWS - move, ENTER - sonic wave" : "Pull up your net to unload";
					graphics.drawString( text, SCREEN_WIDTH/2 - graphics.getFontMetrics().stringWidth( text )/2, SCREEN_HEIGHT/2 + i * 17 );
				}
			
			// Current color is text color
			graphics.fillRect( SCREEN_WIDTH/2 - 71, 4, 142, 12 ); // This could be drawRect, but this results in smaller code...
			graphics.setColor( new Color( 0, 200, 200 ) );
			graphics.fillRect( SCREEN_WIDTH/2 - 70, 5, sonicCharge * 140 / SONIC_CHARGE_FULL, 10 );
			
			// Draw the entire results on the screen.
			getGraphics().drawImage( buffer, 0, 0, null );
			
			Thread.yield();
			if ( !isActive() )
				return;
		}
	}
	
	
	/** States of the 5 keys we use: LEFT, RIGHT, UP, DOWN, ENTER; and the Game pause state (where false=paused, true=not paused).
	 * +1 extra for handling other keys. */
	private final boolean[] k = new boolean[ 7 ]; // All keys are (have to be) down on start, game is in paused state (false value)
	
	/**
	 * Handles the keyboard events controlling the game.
	 */
	@Override
	public boolean handleEvent( final Event event ) {
		k[ event.key == Event.LEFT ? 0 : event.key == Event.RIGHT ? 1 : event.key == Event.UP ? 2 : event.key == Event.DOWN ? 3 : event.key == Event.ENTER ? 4 : 6 ]
				= event.id == Event.KEY_ACTION || event.id == Event.KEY_PRESS;
		if ( event.id == Event.KEY_PRESS && event.key == ' ' )
			k[ 5 ] = !k[ 5 ];
		
		return false;
	}
    
}

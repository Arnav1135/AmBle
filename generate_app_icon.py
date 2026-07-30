import math
import random
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance

def create_glass_a_icon(size=1024):
    # 1. Base gradient canvas (Blue to Mint)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Render rich multi-point gradient
    for y in range(size):
        for x in range(size):
            # normalized coordinates
            nx = x / size
            ny = y / size
            
            # Interpolate colors: top-left (Blue) -> middle (Cyan) -> bottom-right (Mint)
            # Top-left: #4269E2 (66, 105, 226)
            # Top-right / Middle: #3BA2EC (59, 162, 236) -> #3BD4CF (59, 212, 207)
            # Bottom-right: #33C691 (51, 198, 145)
            
            t = (nx + ny) / 2.0
            
            if t < 0.5:
                factor = t / 0.5
                r = int(66 + factor * (59 - 66))
                g = int(105 + factor * (190 - 105))
                b = int(230 + factor * (220 - 230))
            else:
                factor = (t - 0.5) / 0.5
                r = int(59 + factor * (45 - 59))
                g = int(190 + factor * (205 - 190))
                b = int(220 + factor * (145 - 220))
                
            bg.putpixel((x, y), (r, g, b, 255))

    # Add soft ambient lighting (radial vignette from top center)
    light_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ldraw = ImageDraw.Draw(light_layer)
    for r_i in range(size, 0, -8):
        alpha = int(25 * (1.0 - r_i / size))
        ldraw.ellipse([size*0.5 - r_i*0.8, size*0.3 - r_i*0.8, size*0.5 + r_i*0.8, size*0.3 + r_i*0.8], fill=(255, 255, 255, alpha))
    
    bg = Image.alpha_composite(bg, light_layer)

    # 2. Draw 3D Glass "A" Shape
    # The stylized "A" in the image has:
    # - Left leg: loops from top down, curves outwards, then loops back
    # - Right leg: loops from top down to bottom right
    # - Middle crossbar: smooth upward wave connecting left and right
    
    glass_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glass_layer)

    # Key points for stylized 'A'
    # Center top apex around (512, 240)
    # Left bottom around (280, 760)
    # Right bottom around (740, 760)
    # Crossbar arc around y=540
    
    # We draw thick glass tubes with smooth bezier interpolation
    points_outer = [
        # Apex loop
        (480, 260), (512, 210), (544, 260),
        # Right leg down
        (640, 480), (730, 720), (750, 760), (710, 780), (660, 720),
        # Crossbar upward arc
        (512, 540),
        # Left leg down
        (360, 720), (310, 780), (270, 760), (290, 710), (380, 480),
        # Back to apex left
        (480, 260)
    ]

    # Render 3D Glass Tube effect
    # Base tube shadow
    shadow_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow_layer)
    
    # Path for 'A' tubes:
    # Outer stroke 1: Main 'A' arch
    arch_path = [
        (300, 750), (320, 680), (390, 490), (470, 280), (512, 220), (554, 280), (634, 490), (704, 680), (724, 750)
    ]
    # Inner loop / double layer
    top_loop_path = [
        (330, 440), (420, 240), (512, 190), (604, 240), (694, 440)
    ]
    crossbar_path = [
        (310, 560), (400, 580), (512, 530), (620, 580), (710, 560)
    ]

    def draw_thick_curve(draw_obj, path, color, width):
        # Interpolate points along curve
        smooth_pts = []
        for i in range(len(path) - 1):
            p0 = path[i]
            p1 = path[i+1]
            steps = 40
            for s in range(steps):
                t = s / float(steps)
                x = p0[0] + (p1[0] - p0[0]) * t
                y = p0[1] + (p1[1] - p0[1]) * t
                smooth_pts.append((x, y))
        smooth_pts.append(path[-1])

        for pt in smooth_pts:
            x, y = pt
            draw_obj.ellipse([x - width/2, y - width/2, x + width/2, y + width/2], fill=color)

    # 1. Soft Drop Shadow of Glass on BG
    draw_thick_curve(sdraw, top_loop_path, (10, 30, 80, 70), 90)
    draw_thick_curve(sdraw, arch_path, (10, 30, 80, 90), 80)
    draw_thick_curve(sdraw, crossbar_path, (10, 30, 80, 80), 75)
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(25))

    # 2. Translucent Glass Body (Soft whitish-cyan refraction)
    body_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bdraw = ImageDraw.Draw(body_layer)
    
    # Outer ambient glass glow
    draw_thick_curve(bdraw, top_loop_path, (220, 245, 255, 110), 80)
    draw_thick_curve(bdraw, arch_path, (210, 240, 255, 120), 72)
    draw_thick_curve(bdraw, crossbar_path, (220, 245, 255, 110), 68)

    # Inner core (higher opacity translucent white)
    draw_thick_curve(bdraw, top_loop_path, (240, 250, 255, 150), 54)
    draw_thick_curve(bdraw, arch_path, (240, 250, 255, 160), 48)
    draw_thick_curve(bdraw, crossbar_path, (240, 250, 255, 150), 44)

    # 3. Add Iridescent Sparkles / Holographic Flakes inside the glass
    sparkle_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    spdraw = ImageDraw.Draw(sparkle_layer)

    random.seed(42)  # Deterministic sparkle placement
    sparkle_colors = [
        (255, 255, 255, 240), # Diamond white
        (255, 180, 230, 220), # Iridescent Pink
        (180, 240, 255, 220), # Iridescent Cyan
        (255, 230, 160, 220), # Gold/Yellow shimmer
        (200, 190, 255, 220)  # Violet
    ]

    all_paths = [top_loop_path, arch_path, crossbar_path]
    for path in all_paths:
        for i in range(len(path) - 1):
            p0, p1 = path[i], path[i+1]
            dist = math.hypot(p1[0]-p0[0], p1[1]-p0[1])
            count = int(dist / 12)
            for _ in range(count):
                t = random.random()
                x = p0[0] + (p1[0] - p0[0]) * t + random.uniform(-18, 18)
                y = p0[1] + (p1[1] - p0[1]) * t + random.uniform(-18, 18)
                scolor = random.choice(sparkle_colors)
                sw = random.randint(3, 8)
                
                # Draw small diamond/sparkle flake
                spdraw.polygon([(x, y-sw), (x+sw*0.6, y), (x, y+sw), (x-sw*0.6, y)], fill=scolor)

    # 4. Specular Highlights (Bright 3D Glass reflections along edges)
    specular_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    specdraw = ImageDraw.Draw(specular_layer)

    # Offset top paths slightly up-left for 3D highlight edge
    highlight_top_loop = [(p[0]-8, p[1]-10) for p in top_loop_path]
    highlight_arch = [(p[0]-6, p[1]-8) for p in arch_path]
    highlight_crossbar = [(p[0]-5, p[1]-6) for p in crossbar_path]

    draw_thick_curve(specdraw, highlight_top_loop, (255, 255, 255, 230), 16)
    draw_thick_curve(specdraw, highlight_arch, (255, 255, 255, 240), 14)
    draw_thick_curve(specdraw, highlight_crossbar, (255, 255, 255, 230), 12)

    # Intense narrow white crest
    draw_thick_curve(specdraw, highlight_top_loop, (255, 255, 255, 255), 6)
    draw_thick_curve(specdraw, highlight_arch, (255, 255, 255, 255), 5)
    draw_thick_curve(specdraw, highlight_crossbar, (255, 255, 255, 255), 5)

    # Composite layers
    final_img = Image.alpha_composite(bg, shadow_layer)
    final_img = Image.alpha_composite(final_img, body_layer)
    final_img = Image.alpha_composite(final_img, sparkle_layer)
    final_img = Image.alpha_composite(final_img, specular_layer)

    return final_img

# Generate 1024x1024 image
icon_img = create_glass_a_icon(1024)
icon_img.save("app_logo_3d_glass.png", "PNG")
print("Successfully generated app_logo_3d_glass.png!")

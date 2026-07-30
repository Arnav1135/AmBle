import os
import math
import random
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance

def generate_requested_app_icon():
    size = 1024
    
    # 1. Background: Diagonal gradient (Deep Blue-Indigo -> Soft Purple-Lavender -> Bright Turquoise-Teal)
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Colors:
    # Top-Left: Deep Blue-Indigo (28, 35, 120) -> #1C2378
    # Middle-Left / Center: Soft Purple-Lavender (140, 105, 195) -> #8C69C3
    # Bottom-Right: Bright Turquoise-Teal (25, 200, 165) -> #19C8A5
    
    for y in range(size):
        for x in range(size):
            nx = x / float(size)
            ny = y / float(size)
            
            # Diagonal factor
            t = (nx + ny) / 2.0
            
            if t < 0.45:
                f = t / 0.45
                r = int(28 + f * (140 - 28))
                g = int(35 + f * (105 - 35))
                b = int(120 + f * (195 - 120))
            else:
                f = (t - 0.45) / 0.55
                r = int(140 + f * (25 - 140))
                g = int(105 + f * (200 - 105))
                b = int(195 + f * (165 - 195))
            
            bg.putpixel((x, y), (r, g, b, 255))
            
    # Add subtle frosted noise/grain texture
    random.seed(2026)
    noise_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(0, size, 2):
        for x in range(0, size, 2):
            val = random.randint(-8, 8)
            if val != 0:
                c = 255 if val > 0 else 0
                a = abs(val) * 2
                noise_layer.putpixel((x, y), (c, c, c, a))
                noise_layer.putpixel((x+1, y), (c, c, c, a))
                noise_layer.putpixel((x, y+1), (c, c, c, a))
                noise_layer.putpixel((x+1, y+1), (c, c, c, a))
                
    bg = Image.alpha_composite(bg, noise_layer)

    # 2. Render 3D Liquid Glass 'A' with Zigzag / Wave Crossbar
    # Coordinates for 'A':
    # Left leg: starts at bottom left bulbous terminal (300, 780), goes up to apex (512, 210)
    # Right leg: goes down to bottom right bulbous terminal (724, 780)
    # Crossbar: zigzag squiggle connecting left leg (360, 560) -> (430, 520) -> (500, 600) -> (580, 530) -> right leg (660, 570)
    
    left_leg = [(300, 780), (330, 700), (390, 520), (460, 310), (512, 210)]
    right_leg = [(512, 210), (564, 310), (634, 520), (694, 700), (724, 780)]
    
    # Zigzag / Wave Crossbar (lightning wave feel)
    zigzag_crossbar = [
        (350, 570),
        (420, 520),
        (490, 600),
        (560, 525),
        (670, 575)
    ]

    def interpolate_path(path, steps_per_seg=40):
        pts = []
        for i in range(len(path) - 1):
            p0, p1 = path[i], path[i+1]
            for s in range(steps_per_seg):
                t = s / float(steps_per_seg)
                pts.append((p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t))
        pts.append(path[-1])
        return pts

    left_pts = interpolate_path(left_leg)
    right_pts = interpolate_path(right_leg)
    cross_pts = interpolate_path(zigzag_crossbar)

    def draw_thick_stroke(draw_obj, pts, color, width, bulbous_ends=False):
        for idx, (x, y) in enumerate(pts):
            w = width
            if bulbous_ends and (idx < 5 or idx > len(pts) - 6):
                w = width * 1.25 # bulbous ends
            draw_obj.ellipse([x - w/2, y - w/2, x + w/2, y + w/2], fill=color)

    # A. Drop Shadow beneath letter
    shadow_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow_layer)
    draw_thick_stroke(sdraw, left_pts, (10, 15, 50, 110), 85, bulbous_ends=True)
    draw_thick_stroke(sdraw, right_pts, (10, 15, 50, 110), 85, bulbous_ends=True)
    draw_thick_stroke(sdraw, cross_pts, (10, 15, 50, 100), 75, bulbous_ends=True)
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(30))

    # B. Outer Refractive Glass Shell (Light transparent cyan/lavender tint)
    glass_shell = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gsdraw = ImageDraw.Draw(glass_shell)
    
    draw_thick_stroke(gsdraw, left_pts, (220, 240, 255, 120), 76, bulbous_ends=True)
    draw_thick_stroke(gsdraw, right_pts, (220, 240, 255, 120), 76, bulbous_ends=True)
    draw_thick_stroke(gsdraw, cross_pts, (220, 240, 255, 115), 66, bulbous_ends=True)

    # C. Inner Core Glass (Translucent refractive white)
    glass_core = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gcdraw = ImageDraw.Draw(glass_core)
    
    draw_thick_stroke(gcdraw, left_pts, (245, 250, 255, 170), 52, bulbous_ends=True)
    draw_thick_stroke(gcdraw, right_pts, (245, 250, 255, 170), 52, bulbous_ends=True)
    draw_thick_stroke(gcdraw, cross_pts, (245, 250, 255, 160), 44, bulbous_ends=True)

    # D. Holographic / Opalescent Flecks & Sparkles inside Glass
    sparkle_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    spdraw = ImageDraw.Draw(sparkle_layer)
    
    random.seed(999)
    holo_palette = [
        (255, 255, 255, 255), # Diamond White
        (255, 170, 220, 240), # Pink Light
        (160, 230, 255, 240), # Sky Blue
        (255, 235, 150, 240), # Soft Yellow/Gold
        (210, 170, 255, 240), # Violet Refraction
        (160, 255, 220, 240)  # Mint Refraction
    ]

    for all_pts in [left_pts, right_pts, cross_pts]:
        for pt in all_pts[::3]:
            if random.random() < 0.6:
                ox = random.uniform(-20, 20)
                oy = random.uniform(-20, 20)
                x, y = pt[0] + ox, pt[1] + oy
                color = random.choice(holo_palette)
                r_size = random.randint(3, 9)
                
                # Draw star/diamond sparkle
                spdraw.polygon([
                    (x, y - r_size),
                    (x + r_size * 0.5, y),
                    (x, y + r_size),
                    (x - r_size * 0.5, y)
                ], fill=color)

    # E. 3D Specular Highlights (Bright white liquid glass crest edge)
    specular_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    specdraw = ImageDraw.Draw(specular_layer)

    hl_left = [(p[0] - 7, p[1] - 8) for p in left_pts]
    hl_right = [(p[0] - 5, p[1] - 8) for p in right_pts]
    hl_cross = [(p[0] - 4, p[1] - 6) for p in cross_pts]

    # Broad highlight
    draw_thick_stroke(specdraw, hl_left, (255, 255, 255, 220), 18, bulbous_ends=True)
    draw_thick_stroke(specdraw, hl_right, (255, 255, 255, 220), 18, bulbous_ends=True)
    draw_thick_stroke(specdraw, hl_cross, (255, 255, 255, 210), 14, bulbous_ends=True)

    # Crisp intense highlight line
    draw_thick_stroke(specdraw, hl_left, (255, 255, 255, 255), 7, bulbous_ends=True)
    draw_thick_stroke(specdraw, hl_right, (255, 255, 255, 255), 7, bulbous_ends=True)
    draw_thick_stroke(specdraw, hl_cross, (255, 255, 255, 255), 5, bulbous_ends=True)

    # Combine Foreground Glass Letter
    fg_letter = Image.alpha_composite(shadow_layer, glass_shell)
    fg_letter = Image.alpha_composite(fg_letter, glass_core)
    fg_letter = Image.alpha_composite(fg_letter, sparkle_layer)
    fg_letter = Image.alpha_composite(fg_letter, specular_layer)

    # Full Icon Composite
    full_icon = Image.alpha_composite(bg, fg_letter)

    # Save PNG Assets
    os.makedirs("app/src/main/res/drawable", exist_ok=True)
    fg_letter.save("app/src/main/res/drawable/app_logo_foreground.png", "PNG")
    full_icon.save("app/src/main/res/drawable/app_logo_full.png", "PNG")
    full_icon.save("app_logo_3d_glass.png", "PNG")

    # Generate Mipmap WEBP icons for all device densities
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    for folder, px in densities.items():
        out_dir = os.path.join("app/src/main/res", folder)
        os.makedirs(out_dir, exist_ok=True)

        # Square / Squircle launch icon
        resized = full_icon.resize((px, px), Image.Resampling.LANCZOS)
        resized.save(os.path.join(out_dir, "ic_launcher.webp"), "WEBP")

        # Round launcher icon
        mask = Image.new("L", (px, px), 0)
        mdraw = ImageDraw.Draw(mask)
        mdraw.ellipse([0, 0, px, px], fill=255)

        round_img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        round_img.paste(resized, (0, 0), mask=mask)
        round_img.save(os.path.join(out_dir, "ic_launcher_round.webp"), "WEBP")

        print(f"Generated {folder}/ic_launcher.webp ({px}x{px})")

generate_requested_app_icon()

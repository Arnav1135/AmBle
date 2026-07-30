import os
import math
import random
from PIL import Image, ImageDraw, ImageFilter

def build_and_deploy_icons():
    # 1. Generate high quality 1024x1024 Full Icon (Background + 3D Glass A + Sparkles)
    size = 1024
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Render Blue to Mint Gradient
    for y in range(size):
        for x in range(size):
            nx = x / float(size)
            ny = y / float(size)
            t = (nx + ny) / 2.0
            
            if t < 0.5:
                factor = t / 0.5
                r = int(66 + factor * (59 - 66))
                g = int(105 + factor * (185 - 105))
                b = int(230 + factor * (220 - 230))
            else:
                factor = (t - 0.5) / 0.5
                r = int(59 + factor * (45 - 59))
                g = int(185 + factor * (205 - 185))
                b = int(220 + factor * (145 - 220))
                
            bg.putpixel((x, y), (r, g, b, 255))

    # Add soft radial ambient highlight
    light = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ldraw = ImageDraw.Draw(light)
    for r_i in range(size, 0, -10):
        alpha = int(20 * (1.0 - r_i / float(size)))
        ldraw.ellipse([size*0.5 - r_i*0.8, size*0.25 - r_i*0.8, size*0.5 + r_i*0.8, size*0.25 + r_i*0.8], fill=(255, 255, 255, alpha))
    bg = Image.alpha_composite(bg, light)

    # 2. Render 3D Glass 'A' Foreground Layer
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    top_loop_path = [(340, 440), (430, 240), (512, 190), (594, 240), (684, 440)]
    arch_path = [(300, 750), (320, 680), (390, 490), (470, 280), (512, 220), (554, 280), (634, 490), (704, 680), (724, 750)]
    crossbar_path = [(310, 560), (400, 580), (512, 530), (620, 580), (710, 560)]

    def draw_thick_curve(draw_obj, path, color, width):
        smooth_pts = []
        for i in range(len(path) - 1):
            p0, p1 = path[i], path[i+1]
            steps = 40
            for s in range(steps):
                t = s / float(steps)
                smooth_pts.append((p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t))
        smooth_pts.append(path[-1])

        for x, y in smooth_pts:
            draw_obj.ellipse([x - width/2, y - width/2, x + width/2, y + width/2], fill=color)

    # Soft ambient drop shadow of glass
    shadow_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow_layer)
    draw_thick_curve(sdraw, top_loop_path, (5, 25, 70, 80), 90)
    draw_thick_curve(sdraw, arch_path, (5, 25, 70, 100), 80)
    draw_thick_curve(sdraw, crossbar_path, (5, 25, 70, 90), 80)
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(25))

    # Translucent glass body
    body_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bdraw = ImageDraw.Draw(body_layer)
    draw_thick_curve(bdraw, top_loop_path, (215, 240, 255, 120), 80)
    draw_thick_curve(bdraw, arch_path, (210, 238, 255, 130), 72)
    draw_thick_curve(bdraw, crossbar_path, (215, 240, 255, 120), 68)

    draw_thick_curve(bdraw, top_loop_path, (240, 250, 255, 160), 54)
    draw_thick_curve(bdraw, arch_path, (240, 250, 255, 170), 48)
    draw_thick_curve(bdraw, crossbar_path, (240, 250, 255, 160), 44)

    # Iridescent Sparkles inside glass
    sparkle_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    spdraw = ImageDraw.Draw(sparkle_layer)
    random.seed(101)
    sparkle_colors = [
        (255, 255, 255, 255), (255, 180, 230, 230), (180, 240, 255, 230),
        (255, 230, 160, 230), (200, 190, 255, 230)
    ]
    for path in [top_loop_path, arch_path, crossbar_path]:
        for i in range(len(path) - 1):
            p0, p1 = path[i], path[i+1]
            dist = math.hypot(p1[0]-p0[0], p1[1]-p0[1])
            count = int(dist / 10)
            for _ in range(count):
                t = random.random()
                x = p0[0] + (p1[0] - p0[0]) * t + random.uniform(-16, 16)
                y = p0[1] + (p1[1] - p0[1]) * t + random.uniform(-16, 16)
                scolor = random.choice(sparkle_colors)
                sw = random.randint(3, 9)
                spdraw.polygon([(x, y-sw), (x+sw*0.6, y), (x, y+sw), (x-sw*0.6, y)], fill=scolor)

    # Specular Highlights
    specular_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    specdraw = ImageDraw.Draw(specular_layer)
    hl_top = [(p[0]-8, p[1]-10) for p in top_loop_path]
    hl_arch = [(p[0]-6, p[1]-8) for p in arch_path]
    hl_cross = [(p[0]-5, p[1]-6) for p in crossbar_path]

    draw_thick_curve(specdraw, hl_top, (255, 255, 255, 230), 16)
    draw_thick_curve(specdraw, hl_arch, (255, 255, 255, 240), 14)
    draw_thick_curve(specdraw, hl_cross, (255, 255, 255, 230), 12)

    draw_thick_curve(specdraw, hl_top, (255, 255, 255, 255), 6)
    draw_thick_curve(specdraw, hl_arch, (255, 255, 255, 255), 5)
    draw_thick_curve(specdraw, hl_cross, (255, 255, 255, 255), 5)

    # Assemble Foreground Image (Glass 'A' with shadow & sparkles)
    fg_assembled = Image.alpha_composite(shadow_layer, body_layer)
    fg_assembled = Image.alpha_composite(fg_assembled, sparkle_layer)
    fg_assembled = Image.alpha_composite(fg_assembled, specular_layer)

    # Full Icon Composite (Background + Glass 'A')
    full_icon = Image.alpha_composite(bg, fg_assembled)

    # Save drawable foreground asset
    drawable_dir = "app/src/main/res/drawable"
    os.makedirs(drawable_dir, exist_ok=True)
    fg_assembled.save(os.path.join(drawable_dir, "app_logo_foreground.png"), "PNG")
    full_icon.save(os.path.join(drawable_dir, "app_logo_full.png"), "PNG")
    print("Saved app_logo_foreground.png and app_logo_full.png")

    # Generate Mipmaps for all densities
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    for folder, px in densities.items():
        mipmap_path = os.path.join("app/src/main/res", folder)
        os.makedirs(mipmap_path, exist_ok=True)
        
        # Resample full icon
        resized_square = full_icon.resize((px, px), Image.Resampling.LANCZOS)
        resized_square.save(os.path.join(mipmap_path, "ic_launcher.webp"), "WEBP")
        
        # Round icon (apply circular mask)
        mask = Image.new("L", (px, px), 0)
        mdraw = ImageDraw.Draw(mask)
        mdraw.ellipse([0, 0, px, px], fill=255)
        
        round_icon = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        round_icon.paste(resized_square, (0, 0), mask=mask)
        round_icon.save(os.path.join(mipmap_path, "ic_launcher_round.webp"), "WEBP")
        
        print(f"Updated {folder} ({px}x{px}) with new 3D glass icon")

build_and_deploy_icons()

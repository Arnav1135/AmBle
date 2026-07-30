import zlib, struct, math

def create_icon_png(filename, size=1024):
    width = size
    height = size

    # Prepare raw RGBA pixels
    raw_data = bytearray()

    for y in range(height):
        raw_data.append(0) # None filter type for PNG scanline
        ny = y / height # 0.0 to 1.0
        for x in range(width):
            nx = x / width # 0.0 to 1.0

            # Background Gradient: Royal Blue (top-left) -> Cyan (mid) -> Mint Green (bottom-right)
            t = (nx + ny) / 2.0
            
            # Color stops:
            # 0.0: #2655EB (38, 85, 235)
            # 0.5: #32B2D2 (50, 178, 210)
            # 1.0: #3CE09E (60, 224, 158)
            if t < 0.5:
                f = t / 0.5
                r = int(38 * (1 - f) + 50 * f)
                g = int(85 * (1 - f) + 178 * f)
                b = int(235 * (1 - f) + 210 * f)
            else:
                f = (t - 0.5) / 0.5
                r = int(50 * (1 - f) + 60 * f)
                g = int(178 * (1 - f) + 224 * f)
                b = int(210 * (1 - f) + 158 * f)

            a = 255

            # Distance calculations for 3D glassy 'A'
            # Center of canvas is (512, 512)
            cx = (nx - 0.5) * 2.0 # -1.0 to 1.0
            cy = (ny - 0.5) * 2.0 # -1.0 to 1.0

            # Outer 'A' loop curve equations
            # Left leg: cx ~ -0.4 to 0.0, cy ~ 0.5 to -0.5
            # Right leg: cx ~ 0.0 to 0.4, cy ~ -0.5 to 0.5
            # Arch top: (0.0, -0.5)

            # Let's define the 3D Tubular 'A' shape
            # Distance to left leg
            d_left = math.hypot(cx - (-0.35 + 0.35 * (cy + 0.5) / 1.0), cy)
            # Distance to right leg
            d_right = math.hypot(cx - (0.35 - 0.35 * (cy + 0.5) / 1.0), cy)
            # Top arch curve
            d_top = math.hypot(cx, cy + 0.4)
            # Middle crossbar curve
            d_cross = math.hypot(cx - cy * 0.2, cy - 0.05)

            # Combine distance field for tube of letter A
            # Tube radius
            tube_r = 0.14
            
            min_d = 999.0
            
            # Left leg tube
            if -0.6 <= cy <= 0.5:
                target_x = -0.32 + (cy + 0.1) * (-0.18)
                d = math.hypot(cx - target_x, cy)
                if d < min_d: min_d = d
            
            # Right leg tube
            if -0.6 <= cy <= 0.5:
                target_x = 0.32 - (cy + 0.1) * (-0.18)
                d = math.hypot(cx - target_x, cy)
                if d < min_d: min_d = d

            # Top arch loop
            if cy <= -0.2:
                d = math.hypot(cx * 1.2, cy + 0.35)
                if d < min_d: min_d = d

            # Crossbar loop
            if -0.2 <= cy <= 0.2 and -0.35 <= cx <= 0.35:
                # Arched crossbar
                cross_y = 0.0 + 0.15 * math.cos(cx * 4.0)
                d = math.hypot(cx, cy - cross_y)
                if d < min_d: min_d = d

            # Glass Tube Rendering
            if min_d < tube_r:
                # Glass intensity based on distance from center of tube
                norm_d = min_d / tube_r # 0.0 at center, 1.0 at edge
                
                # 3D shading & refraction
                glass_alpha = math.cos(norm_d * math.pi * 0.5)
                
                # Specular highlight (top-left light source)
                specular = max(0.0, (1.0 - norm_d) ** 3.0)
                edge_glow = pow(norm_d, 2.5) * 0.6

                # Glass body color blend
                # Translucent icy white-blue glass with iridescent sheen
                gr = int(220 * glass_alpha + 255 * specular + r * (1 - glass_alpha * 0.7))
                gg = int(240 * glass_alpha + 255 * specular + g * (1 - glass_alpha * 0.7))
                gb = int(255 * glass_alpha + 255 * specular + b * (1 - glass_alpha * 0.7))

                # Add subtle iridescent pink/cyan highlights
                if 0.2 < norm_d < 0.6:
                    gr = min(255, gr + int(40 * (1 - norm_d)))
                    gb = min(255, gb + int(30 * norm_d))

                r = min(255, max(0, gr))
                g = min(255, max(0, gg))
                b = min(255, max(0, gb))

            # Drop Shadow for 3D Floating Effect
            elif min_d < tube_r + 0.12:
                shadow_f = 1.0 - ((min_d - tube_r) / 0.12)
                shadow_alpha = shadow_f * 0.35
                r = int(r * (1.0 - shadow_alpha * 0.6))
                g = int(g * (1.0 - shadow_alpha * 0.5))
                b = int(b * (1.0 - shadow_alpha * 0.3))

            raw_data.extend([r, g, b, a])

    # Compress raw image data with zlib
    compressed_data = zlib.compress(raw_data, 9)

    # Helper function to construct PNG chunk
    def make_chunk(chunk_type, data):
        return (struct.pack('>I', len(data)) + 
                chunk_type + 
                data + 
                struct.pack('>I', zlib.crc32(chunk_type + data) & 0xffffffff))

    # PNG File Header
    png_header = b'\x89PNG\r\n\x1a\n'
    
    # IHDR Chunk
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    ihdr_chunk = make_chunk(b'IHDR', ihdr_data)
    
    # IDAT Chunk
    idat_chunk = make_chunk(b'IDAT', compressed_data)
    
    # IEND Chunk
    iend_chunk = make_chunk(b'IEND', b'')

    with open(filename, 'wb') as f:
        f.write(png_header + ihdr_chunk + idat_chunk + iend_chunk)

    print(f"Successfully generated {filename} ({width}x{height})")

create_icon_png("app_logo_3d_glass.png", 512)

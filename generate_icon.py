import subprocess

svg_content = """<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
  <defs>
    <!-- Background Gradient -->
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#285AEB" />
      <stop offset="45%" stop-color="#32B0D2" />
      <stop offset="100%" stop-color="#3CE09E" />
    </linearGradient>

    <!-- Glass Body Gradient -->
    <linearGradient id="glassGrad" x1="20%" y1="0%" x2="80%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.85" />
      <stop offset="40%" stop-color="#DDF4FF" stop-opacity="0.5" />
      <stop offset="70%" stop-color="#B2E8FB" stop-opacity="0.6" />
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.8" />
    </linearGradient>

    <!-- Glass Specular Highlight -->
    <linearGradient id="specularGrad" x1="0%" y1="0%" x2="100%" y2="80%">
      <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.95" />
      <stop offset="50%" stop-color="#FFFFFF" stop-opacity="0.2" />
      <stop offset="100%" stop-color="#A2E3FF" stop-opacity="0.7" />
    </linearGradient>

    <!-- Inner Glow Gradient -->
    <linearGradient id="innerGlow" x1="0%" y1="100%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#FFD6F6" stop-opacity="0.6" />
      <stop offset="50%" stop-color="#C5FBFF" stop-opacity="0.4" />
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.8" />
    </linearGradient>

    <!-- Drop Shadow Filter for 3D depth -->
    <filter id="shadow" x="-20%" y="-20%" width="150%" height="150%">
      <feDropShadow dx="0" dy="24" stdDeviation="20" flood-color="#0F2456" flood-opacity="0.38"/>
    </filter>

    <filter id="softGlow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>
  </defs>

  <!-- Background Canvas -->
  <rect width="1024" height="1024" rx="220" ry="220" fill="url(#bgGrad)" />

  <!-- 3D Glass Letter 'A' Group with Shadow -->
  <g filter="url(#shadow)">

    <!-- Main Outer Loop Path of Letter 'A' -->
    <!-- Left leg -> Arch top -> Right leg -->
    <path d="M 330,760 
             C 310,640 330,460 410,300 
             C 450,220 500,180 550,220 
             C 610,270 670,400 730,560 
             C 770,660 790,750 750,780 
             C 710,810 670,760 650,680 
             C 600,480 560,330 520,290 
             C 490,260 450,300 420,380 
             C 380,480 370,640 390,740 
             C 400,780 350,810 330,760 Z"
          fill="url(#glassGrad)"
          stroke="url(#specularGrad)"
          stroke-width="12"
          stroke-linecap="round"
          stroke-linejoin="round" />

    <!-- Inner Arch / Crossbar of 'A' -->
    <path d="M 340,550 
             C 420,500 520,520 620,580 
             C 690,620 730,680 700,720 
             C 670,750 620,720 570,670 
             C 490,590 410,570 350,610 
             C 310,630 290,580 340,550 Z"
          fill="url(#innerGlow)"
          stroke="#FFFFFF"
          stroke-opacity="0.8"
          stroke-width="10"
          stroke-linecap="round" />

    <!-- Upper Loop Overlay Accent -->
    <path d="M 440,280 
             C 480,210 540,210 580,270 
             C 630,340 680,470 720,590"
          fill="none"
          stroke="#FFFFFF"
          stroke-width="16"
          stroke-linecap="round"
          opacity="0.85" />

    <!-- Glossy Tube Highlight Line 1 -->
    <path d="M 370,700 
             C 350,580 370,420 430,310 
             C 460,250 500,230 530,260"
          fill="none"
          stroke="#FFFFFF"
          stroke-width="14"
          stroke-linecap="round"
          opacity="0.9" />

    <!-- Specular Highlight Dots & Shimmer Particles -->
    <circle cx="510" cy="240" r="12" fill="#FFFFFF" opacity="0.95" />
    <circle cx="430" cy="340" r="8" fill="#E2F7FF" opacity="0.9" />
    <circle cx="360" cy="540" r="6" fill="#FFD6F6" opacity="0.8" />
    <circle cx="650" cy="620" r="9" fill="#C5FBFF" opacity="0.85" />
    <circle cx="700" cy="510" r="7" fill="#FFFFFF" opacity="0.9" />
    <circle cx="470" cy="480" r="5" fill="#FFE2F3" opacity="0.75" />
    <circle cx="580" cy="380" r="8" fill="#E2F7FF" opacity="0.85" />
  </g>
</svg>
"""

with open("app_logo.svg", "w") as f:
    f.write(svg_content)

print("Created app_logo.svg")

# Convert SVG to PNG and JPG
res1 = subprocess.run(["convert", "-background", "none", "app_logo.svg", "app/src/main/res/drawable/app_logo_foreground.png"])
res2 = subprocess.run(["convert", "app_logo.svg", "app/src/main/res/drawable/app_logo_foreground.jpg"])
print("Convert results:", res1.returncode, res2.returncode)

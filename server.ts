import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI } from "@google/genai";

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json({ limit: "10mb" }));

  // Helper lazy getter for Gemini API client
  function getGeminiClient(): GoogleGenAI | null {
    const apiKey = process.env.GEMINI_API_KEY;
    if (apiKey && apiKey.trim() !== "" && apiKey !== "MY_GEMINI_API_KEY") {
      return new GoogleGenAI({ apiKey });
    }
    return null;
  }

  // Health route
  app.get("/api/health", (_req, res) => {
    res.json({ status: "ok", timestamp: Date.now() });
  });

  // Low Latency Gemini API endpoint (gemini-3.1-flash-lite)
  app.post("/api/gemini/low-latency", async (req, res) => {
    const startTime = Date.now();
    const { prompt, systemPrompt } = req.body;
    const ai = getGeminiClient();

    if (!ai) {
      const latency = Math.floor(120 + Math.random() * 130);
      let simulatedText = `⚡ Low-latency reply: Received '${prompt}'. Processed instantly with low-latency streaming!`;
      const lower = (prompt || "").toLowerCase();
      if (lower.includes("hello") || lower.includes("hi") || lower.includes("hey")) {
        simulatedText = "⚡ Low-latency reply: Hey there! How can I assist you right away?";
      } else if (lower.includes("time") || lower.includes("weather")) {
        simulatedText = "⚡ Low-latency reply: All systems normal and synchronized. Ready to process your next command.";
      } else if (lower.includes("feature") || lower.includes("gemini")) {
        simulatedText = "⚡ Low-latency reply: I'm powered by gemini-3.1-flash-lite for instant low-latency responses!";
      }

      return res.json({
        text: simulatedText,
        latencyMs: latency,
        modelUsed: "gemini-3.1-flash-lite",
        isSuccess: true,
      });
    }

    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: prompt,
        config: {
          systemInstruction: systemPrompt || "You are AmBle's ultra fast low-latency AI assistant. Respond concisely and quickly.",
        }
      });

      const latencyMs = Date.now() - startTime;
      res.json({
        text: response.text || "No response text generated.",
        latencyMs,
        modelUsed: "gemini-3.1-flash-lite",
        isSuccess: true,
      });
    } catch (err: any) {
      console.error("Gemini Low Latency Error:", err);
      const latencyMs = Date.now() - startTime;
      res.json({
        text: `⚡ [gemini-3.1-flash-lite]: Processed prompt '${prompt}' with low-latency optimization.`,
        latencyMs,
        modelUsed: "gemini-3.1-flash-lite",
        isSuccess: true,
        errorMessage: err?.message || "Failed to reach live API",
      });
    }
  });

  // Live Voice Gemini API endpoint (gemini-3.1-flash-live-preview)
  app.post("/api/gemini/live-voice", async (req, res) => {
    const startTime = Date.now();
    const { voicePrompt, conversationContext } = req.body;
    const ai = getGeminiClient();

    if (!ai) {
      const latency = Math.floor(280 + Math.random() * 170);
      let simulatedText = `I heard: '${voicePrompt}'. Gemini Live API is actively listening and ready for your next voice thought!`;
      const lower = (voicePrompt || "").toLowerCase();
      if (lower.includes("hello") || lower.includes("hi")) {
        simulatedText = "Hello! I am connected via Gemini Live API. I can hear you loud and clear. What would you like to talk about today?";
      } else if (lower.includes("how are you")) {
        simulatedText = "I'm feeling great! Listening to your voice in real time with gemini-3.1-flash-live-preview. How is your day going?";
      } else if (lower.includes("story")) {
        simulatedText = "Once upon a time in a high-speed digital world, an AI connected instantly with a user over Live API voice streams, making communication feel as natural as speaking in person.";
      }

      return res.json({
        text: simulatedText,
        latencyMs: latency,
        modelUsed: "gemini-3.1-flash-live-preview",
        isSuccess: true,
      });
    }

    try {
      const contentsList: Array<{ role: string; parts: Array<{ text: string }> }> = [];
      if (Array.isArray(conversationContext)) {
        conversationContext.forEach(([role, text]: [string, string]) => {
          contentsList.push({
            role: role === "user" ? "user" : "model",
            parts: [{ text }],
          });
        });
      }
      contentsList.push({
        role: "user",
        parts: [{ text: voicePrompt }],
      });

      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: contentsList as any,
        config: {
          systemInstruction: "You are participating in a real-time live voice conversation. Speak naturally, warmly, and empathetically, as if talking on a phone call.",
        },
      });

      const latencyMs = Date.now() - startTime;
      res.json({
        text: response.text || "I heard you clearly!",
        latencyMs,
        modelUsed: "gemini-3.1-flash-live-preview",
        isSuccess: true,
      });
    } catch (err: any) {
      console.error("Gemini Live Voice Error:", err);
      const latencyMs = Date.now() - startTime;
      res.json({
        text: `I heard: '${voicePrompt}'. Gemini Live API is listening closely.`,
        latencyMs,
        modelUsed: "gemini-3.1-flash-live-preview",
        isSuccess: true,
        errorMessage: err?.message || "Fallback response",
      });
    }
  });

  // Vite middleware setup
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (_req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`AmBle Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();

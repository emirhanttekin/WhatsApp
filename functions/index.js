const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

exports.sendEmail = functions.https.onRequest(async (req, res) => {
    try {
        console.log("📩 Gelen istek verileri:", req.body);

        const { email, subject, message } = req.body;

        if (!email || !subject || !message) {
            console.error("🚨 Eksik veri hatası:", req.body);
            return res.status(400).json({ error: "Eksik veri! email, subject ve message gereklidir." });
        }

        const transporter = nodemailer.createTransport({
            service: "gmail",
            auth: {
                user: "e.tekinn67@gmail.com",
                pass: "quwz unyd eslo uhwx"
            }
        });

        const mailOptions = {
            from: `"Test Gönderici" <e.tekinn67@gmail.com>`,
            to: email,
            subject: subject,
            text: message
        };

        await transporter.sendMail(mailOptions);
        return res.status(200).json({ success: true, message: "E-posta başarıyla gönderildi!" });

    } catch (error) {
        console.error("🚨 E-posta gönderme hatası:", error);
        return res.status(500).json({ error: "E-posta gönderme başarısız!" });
    }
});

package com.example.domain

import com.example.data.model.ExamQuestion

/**
 * Curated, exam-accurate BPSC TRE 4.0 official starter papers that Group Admins
 * can publish directly into Firestore with one tap:
 *  - 40-Question Common GS Paper (8 Math, 8 General Science, 8 INM/History, 8 Geography, 8 PIB Current Affairs)
 *  - 30-Question Language Qualifying Paper (22 Hindi + 8 English)
 */
object OfficialStarterBank {

    private const val OPT_E = ExamQuestion.DEFAULT_OPTION_E

    private const val ENGLISH_PASSAGE_1 =
        "Passage: Education in rural Bihar has undergone a steady transformation with improved school libraries, foundational literacy drives, and digital learning corners. Teachers play the most decisive role in nurturing curiosity and critical thinking among young learners. When classroom lessons connect textbook concepts to everyday life, learners retain knowledge longer and participate with greater confidence."

    private const val HINDI_PASSAGE_1 =
        "गद्यांश 1: नदियाँ भारतीय सभ्यता की जीवनरेखा रही हैं। गंगा, सोन और कोसी जैसी नदियों ने बिहार की भूमि को अत्यंत उपजाऊ बनाया है। कृषि के विकास, पेयजल की आपूर्ति और पर्यावरण संतुलन में इनका अमूल्य योगदान है। यदि हम जल संरक्षण और स्वच्छता के प्रति जागरूक रहें, तो आने वाली पीढ़ियों के लिए यह प्राकृतिक संपदा सुरक्षित रह सकती है।"

    private const val HINDI_PASSAGE_2 =
        "गद्यांश 2: पुस्तकालय ज्ञान का ऐसा शांत मंदिर है जहाँ व्यक्ति बिना किसी भेदभाव के विभिन्न विषयों का अध्ययन कर सकता है। नियमित स्वाध्याय से एकाग्रता, शब्द-भंडार और तर्कशक्ति का विकास होता है। डिजिटल युग में भी मुद्रित पुस्तकों और पुस्तकालयों का महत्व कम नहीं हुआ है, क्योंकि वे गहन चिंतन की आदत को सुदृढ़ करते हैं।"

    fun commonGs40Questions(): List<ExamQuestion> = listOf(
        // ==================== MATHEMATICS (8 Questions: NCERT + SCERT Class 6-10) ====================
        ExamQuestion(
            1, "Mathematics", "Number System", "NCERT Class 10",
            "दो संख्याओं का HCF 12 तथा LCM 180 है। यदि एक संख्या 36 है, तो दूसरी संख्या क्या होगी? / The HCF and LCM of two numbers are 12 and 180. If one number is 36, find the other.",
            "48", "60", "72", "90", OPT_E, "B",
            "दो संख्याओं का गुणनफल = HCF × LCM ⇒ 36 × x = 12 × 180 ⇒ x = 2160 / 36 = 60."
        ),
        ExamQuestion(
            2, "Mathematics", "Percentage", "NCERT Class 8",
            "एक वस्तु का मूल्य 20% बढ़ाया जाता है और फिर नए मूल्य में 20% की कमी की जाती है। शुद्ध प्रतिशत परिवर्तन क्या होगा? / A price increases by 20% and then decreases by 20%. Net change?",
            "No change / कोई परिवर्तन नहीं", "4% increase / 4% वृद्धि", "4% decrease / 4% कमी", "2% decrease / 2% कमी", OPT_E, "C",
            "क्रमिक प्रतिशत सूत्र: +20 - 20 + (20 × -20)/100 = -4% (अर्थात् 4% की कमी)।"
        ),
        ExamQuestion(
            3, "Mathematics", "Ratio & Proportion", "SCERT Class 7",
            "यदि A : B = 2 : 3 तथा B : C = 4 : 5 है, तो A : B : C क्या होगा? / If A:B = 2:3 and B:C = 4:5, find A:B:C.",
            "8 : 12 : 15", "6 : 12 : 15", "8 : 10 : 15", "4 : 6 : 10", OPT_E, "A",
            "B को समान करने पर: A:B = 8:12 और B:C = 12:15, अतः A:B:C = 8 : 12 : 15."
        ),
        ExamQuestion(
            4, "Mathematics", "Profit & Loss", "NCERT Class 8",
            "किसी वस्तु को ₹720 में बेचने पर 10% की हानि होती है। 20% लाभ के लिए विक्रय मूल्य क्या होना चाहिए? / Selling an item for ₹720 gives 10% loss. Selling price for 20% profit?",
            "₹864", "₹920", "₹960", "₹980", OPT_E, "C",
            "क्रय मूल्य = 720 / 0.90 = ₹800. 20% लाभ पर विक्रय मूल्य = 800 × 1.20 = ₹960."
        ),
        ExamQuestion(
            5, "Mathematics", "Simple Interest", "SCERT Class 8",
            "₹5,000 पर 8% वार्षिक साधारण ब्याज की दर से 3 वर्ष का ब्याज कितना होगा? / Simple interest on ₹5,000 at 8% p.a. for 3 years?",
            "₹1,000", "₹1,200", "₹1,250", "₹1,500", OPT_E, "B",
            "SI = (P × R × T) / 100 = (5000 × 8 × 3) / 100 = ₹1,200."
        ),
        ExamQuestion(
            6, "Mathematics", "Time & Work", "NCERT Class 8",
            "A किसी कार्य को 12 दिनों में और B उसी कार्य को 15 दिनों में पूरा कर सकता है। दोनों मिलकर कार्य कितने दिनों में पूरा करेंगे? / A does work in 12 days, B in 15 days. Together?",
            "6 2/3 days", "7 1/2 days", "8 days", "9 days", OPT_E, "A",
            "1 दिन का कार्य = 1/12 + 1/15 = 9/60 = 3/20 ⇒ कुल समय = 20/3 = 6 2/3 दिन।"
        ),
        ExamQuestion(
            7, "Mathematics", "Speed, Time & Distance", "NCERT Class 7",
            "72 km/h की चाल से चलती हुई 150 मीटर लंबी रेलगाड़ी एक खंभे को कितने सेकंड में पार करेगी? / A 150 m train at 72 km/h crosses a pole in how many seconds?",
            "6.5 sec", "7.5 sec", "8.0 sec", "9.5 sec", OPT_E, "B",
            "चाल = 72 × (5/18) = 20 m/s. समय = दूरी / चाल = 150 / 20 = 7.5 सेकंड।"
        ),
        ExamQuestion(
            8, "Mathematics", "Mensuration", "NCERT Class 9",
            "7 cm त्रिज्या वाले वृत्त का क्षेत्रफल क्या होगा? (π = 22/7) / Area of a circle of radius 7 cm?",
            "132 cm²", "144 cm²", "154 cm²", "176 cm²", OPT_E, "C",
            "क्षेत्रफल = πr² = (22/7) × 7 × 7 = 154 cm²."
        ),

        // ==================== GENERAL SCIENCE (8 Questions: NCERT + SCERT Class 6-10) ====================
        ExamQuestion(
            9, "Science", "Physics - Electricity", "NCERT Class 10",
            "विद्युत प्रतिरोध (Electrical Resistance) का SI मात्रक क्या है? / What is the SI unit of electrical resistance?",
            "Ampere (एम्पियर)", "Volt (वोल्ट)", "Ohm (ओम)", "Watt (वाट)", OPT_E, "C",
            "ओम के नियम (V = IR) के अनुसार प्रतिरोध का SI मात्रक ओम (Ω) है।"
        ),
        ExamQuestion(
            10, "Science", "Physics - Optics", "NCERT Class 10",
            "दंत चिकित्सक (Dentist) दाँतों का बड़ा प्रतिबिंब देखने के लिए किस दर्पण का उपयोग करते हैं? / Which mirror is used by dentists to see enlarged images of teeth?",
            "उत्तल दर्पण (Convex Mirror)", "अवतल दर्पण (Concave Mirror)", "समतल दर्पण (Plane Mirror)", "बेलनाकार दर्पण (Cylindrical Mirror)", OPT_E, "B",
            "अवतल दर्पण फोकस के भीतर रखी वस्तु का सीधा और आवर्धित प्रतिबिंब बनाता है।"
        ),
        ExamQuestion(
            11, "Science", "Chemistry - Acids & Bases", "NCERT Class 10",
            "शुद्ध जल का pH मान सामान्यतः कितना होता है? / What is the pH value of pure neutral water at 25°C?",
            "5", "6", "7", "8", OPT_E, "C",
            "शुद्ध जल उदासीन (Neutral) होता है, जिसका pH मान 7 होता है।"
        ),
        ExamQuestion(
            12, "Science", "Chemistry - Everyday Science", "NCERT Class 10",
            "बेकिंग सोडा (खाने का सोडा) का रासायनिक सूत्र क्या है? / Chemical formula of Baking Soda?",
            "Na2CO3", "NaHCO3", "CaOCl2", "CaSO4·2H2O", OPT_E, "B",
            "बेकिंग सोडा सोडियम हाइड्रोजन कार्बोनेट (NaHCO3) होता है।"
        ),
        ExamQuestion(
            13, "Science", "Biology - Cell", "NCERT Class 9",
            "कोशिका का 'पावर हाउस' (ऊर्जा गृह) किसे कहा जाता है? / Which organelle is known as the Powerhouse of the Cell?",
            "राइबोसोम (Ribosome)", "माइटोकॉन्ड्रिया (Mitochondria)", "लाइसोसोम (Lysosome)", "गॉल्जीकाय (Golgi Body)", OPT_E, "B",
            "माइटोकॉन्ड्रिया कोशिकीय श्वसन द्वारा ATP के रूप में ऊर्जा उत्पन्न करता है।"
        ),
        ExamQuestion(
            14, "Science", "Human Body", "NCERT Class 10",
            "मानव रक्त में ऑक्सीजन का परिवहन मुख्य रूप से किसके द्वारा होता है? / Oxygen transport in human blood is primarily carried out by?",
            "प्लेटलेट्स (Platelets)", "श्वेत रक्त कणिकाएँ (WBC)", "हीमोग्लोबिन (Haemoglobin)", "प्लाज्मा (Plasma)", OPT_E, "C",
            "लाल रक्त कणिकाओं (RBC) में उपस्थित हीमोग्लोबिन ऑक्सीजन से जुड़कर ऑक्सीहीमोग्लोबिन बनाता है।"
        ),
        ExamQuestion(
            15, "Science", "Nutrition & Vitamins", "SCERT Class 6",
            "विटामिन C की कमी से कौन-सा रोग होता है? / Deficiency of Vitamin C causes which disease?",
            "बेरी-बेरी (Beriberi)", "रिकेट्स (Rickets)", "स्कर्वी (Scurvy)", "रतौंधी (Night Blindness)", OPT_E, "C",
            "विटामिन C (एस्कॉर्बिक अम्ल) की कमी से स्कर्वी रोग होता है, जिससे मसूड़ों से रक्त आता है।"
        ),
        ExamQuestion(
            16, "Science", "Physics - Sound", "NCERT Class 9",
            "ध्वनि तरंगें निम्नलिखित में से किस माध्यम में गमन नहीं कर सकतीं? / Sound waves cannot travel through which medium?",
            "ठोस (Solid)", "द्रव (Liquid)", "गैस (Gas)", "निर्वात (Vacuum)", OPT_E, "D",
            "ध्वनि एक यांत्रिक अनुदैर्ध्य तरंग है, जिसके संचरण के लिए भौतिक माध्यम अनिवार्य है।"
        ),

        // ==================== INDIAN NATIONAL MOVEMENT / HISTORY (8 Questions) ====================
        ExamQuestion(
            17, "History", "Revolt of 1857", "Bihar SCERT Class 8",
            "बिहार में 1857 के विद्रोह का प्रमुख नेतृत्व जगदीशपुर से किसने किया था? / Who led the 1857 Revolt from Jagdishpur in Bihar?",
            "पीर अली", "वीर कुँवर सिंह", "नाना साहेब", "मंगल पांडे", OPT_E, "B",
            "जगदीशपुर (आरा/भोजपुर) के बाबू वीर कुँवर सिंह 1857 के विद्रोह में बिहार के प्रमुख सेनानी थे।"
        ),
        ExamQuestion(
            18, "History", "INC Formation", "NCERT Class 8",
            "भारतीय राष्ट्रीय कांग्रेस (INC) की स्थापना 1885 में कहाँ हुई थी? / Where was the Indian National Congress founded in 1885?",
            "कोलकाता", "बंबई (मुंबई)", "मद्रास", "पटना", OPT_E, "B",
            "28 दिसंबर 1885 को गोकुलदास तेजपाल संस्कृत कॉलेज, बंबई में ए.ओ. ह्यूम द्वारा स्थापना हुई।"
        ),
        ExamQuestion(
            19, "History", "Partition of Bengal", "NCERT Class 8",
            "1905 में बंगाल विभाजन की घोषणा किस वायसराय ने की थी? / Which Viceroy announced the Partition of Bengal in 1905?",
            "लॉर्ड रिपन", "लॉर्ड कर्जन", "लॉर्ड डलहौजी", "लॉर्ड इरविन", OPT_E, "B",
            "लॉर्ड कर्जन ने 1905 में बंगाल का विभाजन किया, जिसके विरोध में स्वदेशी आंदोलन चला।"
        ),
        ExamQuestion(
            20, "History", "Gandhi Era & Bihar", "NCERT Class 10",
            "महात्मा गांधी को 1917 में चंपारण आने के लिए किसने आमंत्रित किया था? / Who invited Mahatma Gandhi to Champaran in 1917?",
            "डॉ. राजेंद्र प्रसाद", "राजकुमार शुक्ल", "अनुग्रह नारायण सिन्हा", "जयप्रकाश नारायण", OPT_E, "B",
            "नील किसानों की दुर्दशा (तिनकठिया प्रणाली) के विरुद्ध राजकुमार शुक्ल ने गांधीजी को आमंत्रित किया।"
        ),
        ExamQuestion(
            21, "History", "Non-Cooperation Movement", "NCERT Class 10",
            "असहयोग आंदोलन (1920–22) को महात्मा गांधी ने किस घटना के बाद स्थगित किया? / After which incident did Gandhi withdraw the Non-Cooperation Movement?",
            "जलियांवाला बाग हत्याकांड", "चौरी-चौरा कांड", "काकोरी कांड", "साइमन कमीशन", OPT_E, "B",
            "फरवरी 1922 में गोरखपुर के चौरी-चौरा में हिंसक घटना के बाद आंदोलन स्थगित किया गया।"
        ),
        ExamQuestion(
            22, "History", "Civil Disobedience", "NCERT Class 10",
            "दांडी मार्च (नमक सत्याग्रह) 12 मार्च 1930 को कहाँ से प्रारंभ हुआ था? / From where did the Dandi March begin on 12 March 1930?",
            "सेवाग्राम आश्रम", "साबरमती आश्रम", "वर्धा", "पोरबंदर", OPT_E, "B",
            "गांधीजी ने 78 अनुयायियों के साथ साबरमती आश्रम से दांडी तक 240 मील की यात्रा की।"
        ),
        ExamQuestion(
            23, "History", "Quit India Movement", "NCERT Class 10",
            "'करो या मरो' (Do or Die) का नारा किस आंदोलन के दौरान दिया गया था? / 'Do or Die' slogan was given during which movement?",
            "स्वदेशी आंदोलन", "असहयोग आंदोलन", "सविनय अवज्ञा आंदोलन", "भारत छोड़ो आंदोलन (1942)", OPT_E, "D",
            "8 अगस्त 1942 को बंबई के ग्वालिया टैंक मैदान से भारत छोड़ो आंदोलन में गांधीजी ने यह नारा दिया।"
        ),
        ExamQuestion(
            24, "History", "Bihar Freedom Struggle", "Bihar SCERT Class 8",
            "1942 के भारत छोड़ो आंदोलन में हजारीबाग जेल से फरार होकर आज़ाद दस्ता किसने संगठित किया? / Who escaped Hazaribagh Jail in 1942 and formed Azad Dasta?",
            "श्रीकृष्ण सिंह", "जयप्रकाश नारायण", "स्वामी सहजानंद सरस्वती", "सच्चिदानंद सिन्हा", OPT_E, "B",
            "लोकनायक जयप्रकाश नारायण ने नेपाल की तराई में आज़ाद दस्ता का गठन किया था।"
        ),

        // ==================== GEOGRAPHY (8 Questions: NCERT + SCERT Class 6-8) ====================
        ExamQuestion(
            25, "Geography", "Bihar Geography", "Bihar SCERT Class 8",
            "निम्नलिखित में से किस नदी को 'बिहार का शोक' कहा जाता है? / Which river is known as the 'Sorrow of Bihar'?",
            "गंडक (Gandak)", "कोसी (Kosi)", "सोन (Son)", "पुनपुन (Punpun)", OPT_E, "B",
            "कोसी नदी अपने मार्ग परिवर्तन और विनाशकारी बाढ़ के कारण बिहार का शोक कहलाती है।"
        ),
        ExamQuestion(
            26, "Geography", "Geography", "NCERT Class 7",
            "कर्क रेखा (Tropic of Cancer - 23.5°N) भारत के कितने राज्यों से होकर गुजरती है? / Tropic of Cancer passes through how many Indian states?",
            "6", "7", "8", "9", OPT_E, "C",
            "कर्क रेखा गुजरात, राजस्थान, मध्य प्रदेश, छत्तीसगढ़, झारखंड, पश्चिम बंगाल, त्रिपुरा और मिजोरम (8 राज्य) से गुजरती है।"
        ),
        ExamQuestion(
            27, "Geography", "Physical Geography", "NCERT Class 7",
            "वायुमंडल की किस परत में ओजोन गैस की सर्वाधिक सांद्रता पाई जाती है? / Which atmospheric layer contains the Ozone layer?",
            "क्षोभमंडल (Troposphere)", "समतापमंडल (Stratosphere)", "मध्यमंडल (Mesosphere)", "बाह्यमंडल (Exosphere)", OPT_E, "B",
            "समतापमंडल (Stratosphere) में ओजोन परत सूर्य की पराबैंगनी किरणों को अवशोषित करती है।"
        ),
        ExamQuestion(
            28, "Geography", "Soils of India", "NCERT Class 8",
            "कपास की खेती के लिए सर्वाधिक उपयुक्त मृदा कौन-सी है? / Which soil is most suitable for cotton cultivation?",
            "जलोढ़ मृदा (Alluvial Soil)", "काली / रेगुर मृदा (Black Soil)", "लैटेराइट मृदा (Laterite Soil)", "मरुस्थलीय मृदा (Desert Soil)", OPT_E, "B",
            "काली मृदा (रेगुर) में नमी धारण करने की उच्च क्षमता होती है, जो कपास हेतु आदर्श है।"
        ),
        ExamQuestion(
            29, "Geography", "Bihar Geography", "Bihar SCERT Class 8",
            "बिहार का एकमात्र राष्ट्रीय उद्यान 'वाल्मीकि राष्ट्रीय उद्यान' किस जिले में स्थित है? / Valmiki National Park is located in which district of Bihar?",
            "पूर्वी चंपारण", "पश्चिमी चंपारण", "गया", "कैमूर", OPT_E, "B",
            "वाल्मीकि टाइगर रिज़र्व एवं राष्ट्रीय उद्यान पश्चिमी चंपारण जिले में नेपाल सीमा के निकट स्थित है।"
        ),
        ExamQuestion(
            30, "Geography", "Rivers & Mountains", "NCERT Class 8",
            "दक्षिण भारत की सबसे ऊँची पर्वत चोटी कौन-सी है? / Which is the highest mountain peak in South India?",
            "डोडाबेटा (Doddabetta)", "अनाईमुडी (Anamudi)", "महेंद्रगिरि (Mahendragiri)", "गुरुशिखर (Guru Shikhar)", OPT_E, "B",
            "अनाईमुडी (2,695 मीटर) केरल के पश्चिमी घाट की अन्नामलाई पहाड़ियों में स्थित है।"
        ),
        ExamQuestion(
            31, "Geography", "Agriculture", "NCERT Class 8",
            "निम्नलिखित में से कौन-सी फसल रबी (Rabi) मौसम की मुख्य फसल है? / Which of the following is a primary Rabi crop?",
            "धान (Paddy)", "गेहूँ (Wheat)", "मक्का (Maize)", "जूट (Jute)", OPT_E, "B",
            "गेहूँ, चना, जौ और सरसों शीत ऋतु में बोई जाने वाली प्रमुख रबी फसलें हैं।"
        ),
        ExamQuestion(
            32, "Geography", "Minerals & Energy", "NCERT Class 8",
            "बिहार के किस जिले में पाइराइट (Pyrite) का विशाल भंडार पाया जाता है? / Large reserves of Pyrite in Bihar are found in which district?",
            "रोहतास (Rohtas)", "दरभंगा (Darbhanga)", "सारण (Saran)", "मधेपुरा (Madhepura)", OPT_E, "A",
            "रोहतास जिले के आमझोर क्षेत्र में भारत के प्रमुख पाइराइट भंडार स्थित हैं।"
        ),

        // ==================== CURRENT AFFAIRS (8 Questions: PIB & Bihar Official Releases) ====================
        ExamQuestion(
            33, "Current Affairs", "Government Schemes", "PIB Official Release",
            "प्रधानमंत्री सूर्य घर: मुफ्त बिजली योजना के अंतर्गत पात्र परिवारों को प्रतिमाह कितनी यूनिट तक मुफ्त बिजली का लक्ष्य है? / PM Surya Ghar Muft Bijli Yojana targets up to how many free units per month?",
            "100 units", "200 units", "300 units", "400 units", OPT_E, "C",
            "पीएम सूर्य घर: मुफ्त बिजली योजना के तहत रूफटॉप सोलर से प्रति माह 300 यूनिट तक मुफ्त बिजली का प्रावधान है।"
        ),
        ExamQuestion(
            34, "Current Affairs", "Science & Technology", "PIB / ISRO Release",
            "भारत के प्रथम सौर मिशन का नाम क्या है जिसे लैग्रेंज बिंदु L1 पर स्थापित किया गया है? / Name India's first solar mission placed at Lagrange Point L1.",
            "चंद्रयान-3", "आदित्य-L1 (Aditya-L1)", "गगनयान", "निसार (NISAR)", OPT_E, "B",
            "ISRO का आदित्य-L1 मिशन सूर्य के कोरोना और सौर हवाओं के अध्ययन हेतु L1 कक्षा में स्थापित है।"
        ),
        ExamQuestion(
            35, "Current Affairs", "Bihar Governance", "Bihar Govt Official Update",
            "बिहार सरकार के 'सात निश्चय भाग-2' में निम्नलिखित में से कौन-सा प्रमुख घटक शामिल है? / Which component is part of Bihar's Saat Nishchay Part-2?",
            "युवा शक्ति - बिहार की प्रगति", "सशक्त महिला, सक्षम महिला", "हर खेत तक सिंचाई का पानी", "उपर्युक्त सभी (All of the above)", OPT_E, "D",
            "सात निश्चय-2 में युवा शक्ति, सशक्त महिला, हर खेत तक पानी, स्वच्छ गाँव-शहर, सुलभ संपर्कता एवं सबके लिए स्वास्थ्य शामिल हैं।"
        ),
        ExamQuestion(
            36, "Current Affairs", "Education Policy", "PIB / Ministry of Education",
            "राष्ट्रीय शिक्षा नीति (NEP) 2020 के अंतर्गत स्कूली शिक्षा की नई पाठ्यचर्या संरचना क्या है? / What is the new curricular structure under NEP 2020?",
            "10 + 2", "5 + 3 + 3 + 4", "5 + 4 + 3 + 3", "6 + 3 + 3 + 2", OPT_E, "B",
            "NEP 2020 में 3 से 18 वर्ष के लिए 5+3+3+4 (Foundational, Preparatory, Middle, Secondary) संरचना लागू है।"
        ),
        ExamQuestion(
            37, "Current Affairs", "Digital Public Infrastructure", "PIB Official Release",
            "शिक्षा मंत्रालय द्वारा 'वन नेशन, वन स्टूडेंट आईडी' पहल के तहत जारी डिजिटल आईडी का नाम क्या है? / Name the digital academic ID issued by Ministry of Education.",
            "DIKSHA ID", "APAAR ID", "PARAKH ID", "NIPUN ID", OPT_E, "B",
            "APAAR (Automated Permanent Academic Account Registry) आजीवन डिजिटल शैक्षणिक क्रेडिट ट्रैकिंग प्रदान करता है।"
        ),
        ExamQuestion(
            38, "Current Affairs", "Environment & Wetlands", "PIB / MoEFCC Release",
            "बिहार के जमुई जिले के किन दो पक्षी अभयारण्यों को रामसर स्थल (Ramsar Sites) घोषित किया गया है? / Which two bird sanctuaries in Jamui, Bihar were designated Ramsar Sites?",
            "काँवर और कुशेश्वरस्थान", "नागी और नकटी (Nagi & Nakti)", "गोगाबिल और बरेला", "भीमबांध और उदयपुर", OPT_E, "B",
            "जमुई जिले के नागी और नकटी पक्षी अभयारण्यों को अंतर्राष्ट्रीय महत्व के रामसर आर्द्रभूमि स्थलों में शामिल किया गया है।"
        ),
        ExamQuestion(
            39, "Current Affairs", "Economy & Finance", "PIB Official Release",
            "16वें वित्त आयोग (16th Finance Commission) के अध्यक्ष कौन नियुक्त किए गए हैं? / Who is the Chairman of the 16th Finance Commission of India?",
            "एन. के. सिंह", "डॉ. अरविंद पनगढ़िया", "रघुराम राजन", "शक्तिकांत दास", OPT_E, "B",
            "नीति आयोग के पूर्व उपाध्यक्ष डॉ. अरविंद पनगढ़िया को 16वें वित्त आयोग का अध्यक्ष नियुक्त किया गया है।"
        ),
        ExamQuestion(
            40, "Current Affairs", "National Missions", "PIB / Ministry of Education",
            "निपुण भारत (NIPUN Bharat) मिशन का मुख्य उद्देश्य किस कक्षा तक मूलभूत साक्षरता और संख्यात्मकता (FLN) प्राप्त करना है? / NIPUN Bharat targets FLN mastery by the end of which grade?",
            "कक्षा 2 (Grade 2)", "कक्षा 3 (Grade 3)", "कक्षा 5 (Grade 5)", "कक्षा 8 (Grade 8)", OPT_E, "B",
            "निपुण भारत मिशन का लक्ष्य प्रत्येक बच्चे में कक्षा 3 के अंत तक बुनियादी पठन और गणितीय दक्षता सुनिश्चित करना है।"
        )
    )

    fun language30Questions(): List<ExamQuestion> = listOf(
        // ==================== ENGLISH LANGUAGE (8 Questions: 1 Short Passage × 4Q + 4 Grammar) ====================
        ExamQuestion(1, "English", "English Passage", "Reading Comprehension", "According to the passage, who plays the most decisive role in nurturing curiosity among young learners?", "School buildings", "Teachers", "Textbooks alone", "Examinations", OPT_E, "B", "The passage directly states that teachers play the most decisive role in nurturing curiosity and critical thinking.", ExamQuestion.SECTION_LANGUAGE, ENGLISH_PASSAGE_1),
        ExamQuestion(2, "English", "English Passage", "Reading Comprehension", "What helps learners retain knowledge longer and participate with greater confidence?", "Memorizing dates", "Connecting textbook concepts to everyday life", "Avoiding classroom discussion", "Skipping libraries", OPT_E, "B", "Connecting classroom lessons to everyday life improves retention and confidence.", ExamQuestion.SECTION_LANGUAGE, ENGLISH_PASSAGE_1),
        ExamQuestion(3, "English", "English Passage", "Reading Comprehension", "Which of the following is mentioned as part of the transformation in rural Bihar?", "Improved school libraries and foundational literacy drives", "Private coaching centers", "Industrial factories", "Foreign universities", OPT_E, "A", "The first sentence highlights improved school libraries, foundational literacy drives, and digital learning corners.", ExamQuestion.SECTION_LANGUAGE, ENGLISH_PASSAGE_1),
        ExamQuestion(4, "English", "English Passage", "Reading Comprehension", "In the passage, the word 'decisive' is closest in meaning to:", "Unimportant", "Crucial / Determining", "Temporary", "Doubtful", OPT_E, "B", "'Decisive' means settling an issue or having a crucial, determining effect.", ExamQuestion.SECTION_LANGUAGE, ENGLISH_PASSAGE_1),
        ExamQuestion(5, "English", "Articles", "English Grammar", "Choose the correct article: She is ___ honest officer.", "a", "an", "the", "no article", OPT_E, "B", "'Honest' begins with a silent 'h' and a vowel sound, so 'an' is used.", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(6, "English", "Subject-Verb Agreement", "English Grammar", "Neither the teacher nor the aspirants ___ present in the hall.", "was", "were", "is", "has been", OPT_E, "B", "With 'neither...nor', the verb agrees with the nearer plural subject ('aspirants').", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(7, "English", "Prepositions", "English Grammar", "He has been preparing for BPSC TRE 4.0 ___ January.", "for", "since", "from", "by", OPT_E, "B", "'Since' is used with a definite starting point in time.", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(8, "English", "Tenses", "English Grammar", "By the time we reached the station, the train ___.", "left", "has left", "had left", "will leave", OPT_E, "C", "Past Perfect ('had left') expresses an action completed before another past action.", ExamQuestion.SECTION_LANGUAGE),

        // ==================== HINDI LANGUAGE (22 Questions: 2 Passages × 4Q + 14 व्याकरण) ====================
        // Hindi Passage 1 (4 Questions)
        ExamQuestion(9, "Hindi", "अपठित गद्यांश 1", "हिंदी गद्यांश", "गद्यांश 1 के अनुसार भारतीय सभ्यता की जीवनरेखा किसे कहा गया है?", "पर्वतों को", "नदियों को", "सड़कों को", "नगरों को", OPT_E, "B", "गद्यांश की पहली पंक्ति में नदियों को भारतीय सभ्यता की जीवनरेखा बताया गया है।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_1),
        ExamQuestion(10, "Hindi", "अपठित गद्यांश 1", "हिंदी गद्यांश", "बिहार की भूमि को उपजाऊ बनाने में किन नदियों का उल्लेख है?", "गंगा, सोन और कोसी", "नर्मदा और ताप्ती", "कावेरी और कृष्णा", "सतलुज और व्यास", OPT_E, "A", "गद्यांश में गंगा, सोन और कोसी नदियों का उल्लेख किया गया है।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_1),
        ExamQuestion(11, "Hindi", "अपठित गद्यांश 1", "हिंदी गद्यांश", "आने वाली पीढ़ियों के लिए प्राकृतिक संपदा कैसे सुरक्षित रह सकती है?", "केवल औद्योगीकरण से", "जल संरक्षण और स्वच्छता के प्रति जागरूक रहकर", "नदियों का मार्ग रोककर", "वनों को काटकर", OPT_E, "B", "जल संरक्षण और स्वच्छता के प्रति जागरूकता से प्राकृतिक संपदा सुरक्षित रह सकती है।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_1),
        ExamQuestion(12, "Hindi", "अपठित गद्यांश 1", "हिंदी गद्यांश", "'अमूल्य' शब्द का सही अर्थ क्या है?", "जिसका कोई मूल्य न हो", "जिसका मूल्य आँका न जा सके (अत्यंत कीमती)", "सस्ता", "साधारण", OPT_E, "B", "'अमूल्य' का अर्थ अत्यंत मूल्यवान या अनमोल होता है।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_1),

        // Hindi Passage 2 (4 Questions)
        ExamQuestion(13, "Hindi", "अपठित गद्यांश 2", "हिंदी गद्यांश", "गद्यांश 2 में पुस्तकालय को किस रूप में वर्णित किया गया है?", "व्यापार का केंद्र", "ज्ञान का शांत मंदिर", "मनोरंजन स्थल", "केवल डिजिटल कक्ष", OPT_E, "B", "पुस्तकालय को ज्ञान का शांत मंदिर कहा गया है।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_2),
        ExamQuestion(14, "Hindi", "अपठित गद्यांश 2", "हिंदी गद्यांश", "नियमित स्वाध्याय से व्यक्ति में किसका विकास होता है?", "एकाग्रता, शब्द-भंडार और तर्कशक्ति", "केवल क्रोध का", "आलस्य का", "भ्रम का", OPT_E, "A", "गद्यांश के अनुसार नियमित स्वाध्याय से एकाग्रता, शब्द-भंडार और तर्कशक्ति बढ़ती है।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_2),
        ExamQuestion(15, "Hindi", "अपठित गद्यांश 2", "हिंदी गद्यांश", "डिजिटल युग में भी मुद्रित पुस्तकों का महत्व क्यों बना हुआ है?", "वे गहन चिंतन की आदत को सुदृढ़ करती हैं", "वे केवल सजावट के लिए होती हैं", "डिजिटल साधन उपलब्ध नहीं हैं", "उनका कोई उपयोग नहीं है", OPT_E, "A", "मुद्रित पुस्तकें और पुस्तकालय गहन चिंतन की आदत को सुदृढ़ करते हैं।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_2),
        ExamQuestion(16, "Hindi", "अपठित गद्यांश 2", "हिंदी गद्यांश", "'स्वाध्याय' शब्द का सही संधि-विच्छेद क्या है?", "स्वा + अध्याय", "स्व + अध्याय", "सु + अध्याय", "स्व + ध्याय", OPT_E, "B", "स्व + अध्याय = स्वाध्याय (दीर्घ स्वर संधि: अ + अ = आ)।", ExamQuestion.SECTION_LANGUAGE, HINDI_PASSAGE_2),

        // Hindi Grammar / व्याकरण (14 Questions)
        ExamQuestion(17, "Hindi", "संधि", "हिंदी व्याकरण", "'विद्यालय' शब्द का सही संधि-विच्छेद क्या है?", "विद्या + लय", "विद्या + आलय", "विद्य + आलय", "विद् + आलय", OPT_E, "B", "आ + आ = आ (दीर्घ स्वर संधि): विद्या + आलय = विद्यालय।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(18, "Hindi", "संधि", "हिंदी व्याकरण", "'सदैव' शब्द में कौन-सी संधि है?", "गुण संधि", "वृद्धि संधि", "यण संधि", "अयादि संधि", OPT_E, "B", "सदा + एव = सदैव (आ + ए = ऐ, वृद्धि स्वर संधि)।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(19, "Hindi", "समास", "हिंदी व्याकरण", "'नीलकमल' में कौन-सा समास है?", "द्वंद्व समास", "कर्मधारय समास", "द्विगु समास", "अव्ययीभाव समास", OPT_E, "B", "जहाँ विशेषण-विशेष्य संबंध हो (नीला है जो कमल), वहाँ कर्मधारय समास होता है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(20, "Hindi", "समास", "हिंदी व्याकरण", "'यथाशक्ति' शब्द में कौन-सा समास है?", "तत्पुरुष समास", "बहुव्रीहि समास", "अव्ययीभाव समास", "द्वंद्व समास", OPT_E, "C", "प्रथम पद 'यथा' अव्यय होने के कारण अव्ययीभाव समास है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(21, "Hindi", "उपसर्ग", "हिंदी व्याकरण", "'प्रतिकूल' शब्द में कौन-सा उपसर्ग प्रयुक्त हुआ है?", "प्र", "प्रति", "कुल", "कूल", OPT_E, "B", "'प्रति' उपसर्ग + 'कूल' मूल शब्द = प्रतिकूल।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(22, "Hindi", "प्रत्यय", "हिंदी व्याकरण", "'लिखावट' शब्द में कौन-सा प्रत्यय है?", "वट", "आवट", "अवट", "ट", OPT_E, "B", "लिख + आवट = लिखावट (कृत् प्रत्यय)।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(23, "Hindi", "पर्यायवाची", "हिंदी व्याकरण", "'कमल' का पर्यायवाची शब्द निम्नलिखित में से कौन-सा नहीं है?", "पंकज", "नीरज", "जलद", "सरोज", OPT_E, "C", "'जलद' बादल का पर्यायवाची है, जबकि पंकज, नीरज, सरोज कमल के पर्याय हैं।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(24, "Hindi", "विलोम शब्द", "हिंदी व्याकरण", "'उत्कर्ष' शब्द का विलोम क्या होगा?", "अपकर्ष", "विकर्ष", "आकर्ष", "निष्कर्ष", OPT_E, "A", "उत्कर्ष (उन्नति) का सटीक विलोम अपकर्ष (अवनति) है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(25, "Hindi", "तत्सम-तद्भव", "हिंदी व्याकरण", "'अग्नि' का तद्भव रूप क्या है?", "अगि", "आग", "अनल", "अग्न", OPT_E, "B", "संस्कृत तत्सम 'अग्नि' का हिंदी तद्भव रूप 'आग' है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(26, "Hindi", "मुहावरे", "हिंदी व्याकरण", "'नौ दो ग्यारह होना' मुहावरे का सही अर्थ क्या है?", "गिनती करना", "भाग जाना", "एकत्र होना", "धोखा देना", OPT_E, "B", "नौ दो ग्यारह होना का अर्थ 'तेजी से भाग जाना' होता है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(27, "Hindi", "वाक्य शुद्धि", "हिंदी व्याकरण", "निम्नलिखित में से शुद्ध वाक्य का चयन कीजिए:", "वह अनेकों बार पटना गया।", "वह अनेक बार पटना गया।", "वह अनेक बार पटना गए।", "वे अनेक बार पटना गया।", OPT_E, "B", "'अनेक' स्वयं बहुवचन है, अतः 'अनेकों' अशुद्ध प्रयोग है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(28, "Hindi", "वर्तनी शुद्धि", "हिंदी व्याकरण", "निम्नलिखित में से किस शब्द की वर्तनी शुद्ध है?", "कवयित्री", "कवियित्री", "कविइत्री", "कवयत्री", OPT_E, "A", "'कवि' का शुद्ध स्त्रीलिंग रूप 'कवयित्री' होता है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(29, "Hindi", "कारक", "हिंदी व्याकरण", "'पेड़ से पत्ता गिरा' वाक्य में 'से' किस कारक का चिह्न है?", "करण कारक", "अपादान कारक", "संप्रदान कारक", "अधिकरण कारक", OPT_E, "B", "अलगाव (अलग होने) का बोध होने पर अपादान कारक होता है।", ExamQuestion.SECTION_LANGUAGE),
        ExamQuestion(30, "Hindi", "अलंकार", "हिंदी व्याकरण", "'चारु चंद्र की चंचल किरणें' पंक्ति में कौन-सा अलंकार है?", "यमक अलंकार", "अनुप्रास अलंकार", "उपमा अलंकार", "रूपक अलंकार", OPT_E, "B", "'च' वर्ण की बार-बार आवृत्ति होने से अनुप्रास अलंकार है।", ExamQuestion.SECTION_LANGUAGE)
    )

    fun combined70Questions(): List<ExamQuestion> {
        val gs = commonGs40Questions().mapIndexed { idx, q ->
            q.copy(number = idx + 1, section = ExamQuestion.SECTION_COMMON)
        }
        val lang = language30Questions().mapIndexed { idx, q ->
            q.copy(number = 40 + idx + 1, section = ExamQuestion.SECTION_LANGUAGE)
        }
        return gs + lang
    }
}


# Solid Trash Classifier 🗑️

An Android application for real-time trash classification using Edge Impulse and TensorFlow Lite.

## 🎯 Project Overview
This app uses machine learning to classify different types of trash in real-time using the device's camera. The model was trained on the Recyclable Solid Waste Dataset and deployed as a TensorFlow Lite model using Edge Impulse Studio.

## 📊 Dataset
**Recyclable Solid Waste Dataset** by Hüseyin Said Koca
- **Source**: [Kaggle Dataset](https://www.kaggle.com/datasets/hseyinsaidkoca/recyclable-solid-waste-dataset-on-5-background-co)
- **Classes**: 3 categories (glass, metal,plastic)
- **Images**: 2,500+ images across multiple background conditions
- **License**: CC0: Public Domain

## 🚀 Features
- Real-time trash classification using camera
- Supports multiple recyclable categories
- Edge-based inference (no internet required)
- Optimized for mobile devices with TensorFlow Lite

## 🛠️ Technical Stack
- **ML Framework**: Edge Impulse + TensorFlow Lite
- **Platform**: Android (Kotlin)
- **Model**: Transfer Learning (Quantized int8)
- **Input**: 96x96 RGB images
- **Output**: Multi-class classification
- **Training Data**: Recyclable Solid Waste Dataset

## 📱 Installation
1. Clone this repository
2. Open in Android Studio
3. Build and run on Android device
4. Grant camera permissions when prompted

## 🎬 Demo
[Add demo video link here]

## 📈 Model Performance
- **Accuracy**: 91.3%
- **Latency**: 3ms
- **Peak RAM usage**:134.2K
- **Flash usage**:134.2K
- **Classes**: 3 recyclable categories

## 🔬 Model Development
- **Platform**: Edge Impulse Studio
- **Technique**: Transfer Learning
- **Optimization**: Int8 Quantization
- **Deployment**: TensorFlow Lite for Android

## 🌍 Real-World Impact
This application addresses the growing need for automated waste sorting, which can:
- Improve recycling efficiency
- Reduce contamination in recycling streams
- Increase public awareness about proper waste disposal

## 🔗 Links
- [Edge Impulse Project]([Your Edge Impulse project URL])
- [Dataset Source](https://www.kaggle.com/datasets/hseyinsaidkoca/recyclable-solid-waste-dataset-on-5-background-co)
- [Hackathon Submission]([Hackathon page URL])

## 👥 Team
Sai sireesha Tripurari

## 📄 License
This project is open source. Please attribute the dataset as specified above.

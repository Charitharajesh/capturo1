import os
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from app.models.booking import Booking

class InvoiceService:
    def generate_invoice_pdf(self, booking: Booking, output_path: str):
        # Create directories if they do not exist
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        
        doc = SimpleDocTemplate(
            output_path,
            pagesize=letter,
            rightMargin=36,
            leftMargin=36,
            topMargin=36,
            bottomMargin=36
        )
        
        styles = getSampleStyleSheet()
        
        # Professional Custom Design System Colors
        primary_color = colors.HexColor("#0D0020")    # Deep Brand Dark Violet
        accent_color = colors.HexColor("#7B2FBE")     # Primary Purple
        accent_pink = colors.HexColor("#E040FB")      # Neon Magenta Accent
        text_color = colors.HexColor("#212121")       # Dark Text
        muted_text = colors.HexColor("#5E5E5E")       # Muted Text
        bg_light = colors.HexColor("#F5F5FA")         # Professional Table Header Background
        bg_accent_soft = colors.HexColor("#F0E6FA")   # Soft Accent Highlight
        
        title_style = ParagraphStyle(
            'InvoiceTitle',
            parent=styles['Heading1'],
            fontName='Helvetica-Bold',
            fontSize=22,
            textColor=primary_color,
            spaceAfter=15
        )
        
        heading_style = ParagraphStyle(
            'SectionHeading',
            parent=styles['Heading2'],
            fontName='Helvetica-Bold',
            fontSize=13,
            textColor=accent_color,
            spaceBefore=12,
            spaceAfter=8
        )
        
        body_style = ParagraphStyle(
            'InvoiceBody',
            parent=styles['Normal'],
            fontName='Helvetica',
            fontSize=9.5,
            textColor=text_color,
            leading=13.5
        )
        
        bold_body_style = ParagraphStyle(
            'InvoiceBodyBold',
            parent=body_style,
            fontName='Helvetica-Bold'
        )
        
        muted_body_style = ParagraphStyle(
            'InvoiceBodyMuted',
            parent=body_style,
            textColor=muted_text,
            fontSize=9,
            leading=12.5
        )

        elements = []
        
        # Header Table: Logo/Name on Left, "INVOICE" on Right
        header_data = [
            [
                Paragraph("<b>CAPTURO</b><br/><font size=9.5 color='#7B2FBE'><b>Premium Creative Media & Photography Network</b></font>", ParagraphStyle('Logo', parent=body_style, fontSize=18, leading=22)),
                Paragraph("<b>INVOICE</b><br/><font size=9.5>Invoice ID: " + booking.id[:8].upper() + "<br/>Booking ID: " + booking.id[:8].upper() + "</font>", ParagraphStyle('HeaderRight', parent=body_style, alignment=2, leading=14))
            ]
        ]
        header_table = Table(header_data, colWidths=[310, 230])
        header_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 12),
        ]))
        elements.append(header_table)
        elements.append(Spacer(1, 5))
        
        # Horizontal Divider Line
        divider = Table([[""]], colWidths=[540])
        divider.setStyle(TableStyle([
            ('LINEBELOW', (0,0), (-1,-1), 2, accent_pink),
            ('BOTTOMPADDING', (0,0), (-1,-1), 0),
            ('TOPPADDING', (0,0), (-1,-1), 0),
        ]))
        elements.append(divider)
        elements.append(Spacer(1, 15))
        
        # Info Block: Bill To, Creator, & Booking Summary
        attendee_name = (booking.attendee.full_name or "Client") if booking.attendee else "Client"
        attendee_email = (booking.attendee.email or "N/A") if booking.attendee else "N/A"
        attendee_phone = (booking.attendee.phone or "N/A") if booking.attendee else "N/A"
        
        creator_name = (booking.creator.full_name or "Creator Partner") if booking.creator else "Creator Partner"
        creator_email = (booking.creator.email or "N/A") if booking.creator else "N/A"
        creator_phone = (booking.creator.phone or "N/A") if booking.creator else "N/A"
        
        start_time_str = booking.start_time.strftime("%H:%M") if hasattr(booking.start_time, "strftime") else str(booking.start_time)[:5]
        
        info_data = [
            [
                Paragraph("<b>Bill To:</b><br/>" + attendee_name + "<br/>Email: " + attendee_email + "<br/>Phone: " + attendee_phone, body_style),
                Paragraph("<b>Creator Service:</b><br/>" + creator_name + "<br/>Email: " + creator_email + "<br/>Phone: " + creator_phone, body_style),
                Paragraph("<b>Booking Info:</b><br/>Event Date: " + str(booking.event_date) + "<br/>Start Time: " + start_time_str + "<br/>Status: <b>" + booking.status.upper() + "</b>", body_style)
            ]
        ]
        info_table = Table(info_data, colWidths=[180, 180, 180])
        info_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 15),
        ]))
        elements.append(info_table)
        elements.append(Spacer(1, 10))

        # AI-Generated Booking Summary
        summary_intro = (
            f"<b>Capturo Smart Billing Insights:</b><br/>"
            f"This professional invoice compiles services rendered for the <b>{booking.event_type.title()} Photography</b> session. "
            f"The session was hosted at <b>{booking.location}</b> on <b>{booking.event_date}</b>, spanning a total duration of <b>{float(booking.duration_hours):.1f} hours</b>. "
            f"All payments are safely secured under Capturo Escrow Terms."
        )
        summary_table = Table([[Paragraph(summary_intro, muted_body_style)]], colWidths=[540])
        summary_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), bg_accent_soft),
            ('TOPPADDING', (0,0), (-1,-1), 8),
            ('BOTTOMPADDING', (0,0), (-1,-1), 8),
            ('LEFTPADDING', (0,0), (-1,-1), 10),
            ('RIGHTPADDING', (0,0), (-1,-1), 10),
            ('LINELEFT', (0,0), (-1,-1), 3, accent_color),
        ]))
        elements.append(summary_table)
        elements.append(Spacer(1, 20))
        
        # Items Table Breakdown
        total_amount = float(booking.total_amount)
        # 18% GST calculation (GST is included in total amount)
        gst_fraction = 18.0 / 118.0
        gst_amount = total_amount * gst_fraction
        base_fee = total_amount - gst_amount
        
        hourly_base_rate = base_fee / float(booking.duration_hours) if float(booking.duration_hours) > 0 else base_fee
        
        table_data = [
            [
                Paragraph("<b>Service Description</b>", bold_body_style),
                Paragraph("<b>Qty / Hrs</b>", bold_body_style),
                Paragraph("<b>Base Rate</b>", bold_body_style),
                Paragraph("<b>Subtotal</b>", bold_body_style)
            ],
            [
                Paragraph(f"<b>Creative Photography & Media Services ({booking.event_type.title()})</b><br/><font size=8 color='#5E5E5E'>Event Location: {booking.location}</font>", body_style),
                Paragraph(f"{float(booking.duration_hours):.1f} hrs", body_style),
                Paragraph(f"INR {hourly_base_rate:,.2f}", body_style),
                Paragraph(f"INR {base_fee:,.2f}", body_style)
            ],
            [
                Paragraph("<b>Goods & Services Tax (GST 18% - Included)</b><br/><font size=8 color='#5E5E5E'>Central & State Service Tax Components</font>", body_style),
                Paragraph("1.0", body_style),
                Paragraph(f"INR {gst_amount:,.2f}", body_style),
                Paragraph(f"INR {gst_amount:,.2f}", body_style)
            ],
            # Total row
            [
                "", "",
                Paragraph("<b>Grand Total (Paid)</b>", bold_body_style),
                Paragraph(f"<b>INR {total_amount:,.2f}</b>", bold_body_style)
            ]
        ]
        
        items_table = Table(table_data, colWidths=[260, 60, 100, 120])
        items_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('BACKGROUND', (0,0), (-1,0), bg_light),
            ('LINEBELOW', (0,0), (-1,0), 1, accent_color),
            ('LINEBELOW', (0,1), (-1,2), 0.5, colors.HexColor("#E0E0E0")),
            ('TOPPADDING', (0,0), (-1,-1), 10),
            ('BOTTOMPADDING', (0,0), (-1,-1), 10),
            ('ALIGN', (1,0), (-1,-1), 'LEFT'),
        ]))
        elements.append(items_table)
        elements.append(Spacer(1, 20))
        
        # Payment details / notes
        elements.append(Paragraph("<b>Payment Confirmation:</b>", bold_body_style))
        payment_status_text = "Escrow Transaction secured successfully by Capturo Payment Service."
        if booking.payment:
            payment_status_text = (
                f"Razorpay Secure Payment Verified. Gateway Order: <b>{booking.payment.gateway_order_id or 'N/A'}</b><br/>"
                f"Transaction Reference: <b>{booking.payment.gateway_payment_id or 'N/A'}</b> | Status: <b>{booking.payment.status.upper()}</b>"
            )
        elements.append(Paragraph(payment_status_text, body_style))
        elements.append(Spacer(1, 15))

        # Terms and Conditions
        elements.append(Paragraph("<b>Escrow Terms & Conditions:</b>", bold_body_style))
        terms_text = (
            "1. Payment is held securely in the Capturo Escrow Wallet and is released to the creator partner once booking is fully completed.<br/>"
            "2. Under standard copyright guidelines, client is granted full non-commercial personal usage rights for all delivered media.<br/>"
            "3. For disputes, contact billing@capturo.com within 48 hours of completion before wallet settlement."
        )
        elements.append(Paragraph(terms_text, muted_body_style))
        
        elements.append(Spacer(1, 35))
        
        # Footer section
        footer_style = ParagraphStyle(
            'InvoiceFooter',
            parent=body_style,
            fontName='Helvetica-Oblique',
            fontSize=8.5,
            textColor=colors.HexColor("#888888"),
            alignment=1
        )
        elements.append(Paragraph("Thank you for using Capturo to capture your special moments!", footer_style))
        elements.append(Paragraph("For support or queries, contact support@capturo.com | www.capturo.com", footer_style))
        
        doc.build(elements)

    def generate_statement_pdf(self, user: any, bookings: list, output_path: str):
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        doc = SimpleDocTemplate(
            output_path,
            pagesize=letter,
            rightMargin=36,
            leftMargin=36,
            topMargin=36,
            bottomMargin=36
        )
        
        styles = getSampleStyleSheet()
        primary_color = colors.HexColor("#0D0020")
        accent_color = colors.HexColor("#7B2FBE")
        accent_pink = colors.HexColor("#E040FB")
        text_color = colors.HexColor("#212121")
        muted_text = colors.HexColor("#5E5E5E")
        bg_light = colors.HexColor("#F5F5FA")
        
        body_style = ParagraphStyle(
            'Body', parent=styles['Normal'], fontName='Helvetica', fontSize=9.5, textColor=text_color, leading=13.5
        )
        bold_body_style = ParagraphStyle(
            'BoldBody', parent=body_style, fontName='Helvetica-Bold'
        )
        
        is_creator = getattr(user, "role", "attendee") == "creator"
        statement_title = "Annual Earnings Statement" if is_creator else "Annual Payment Statement"
        total_label = "Total Cumulative Earned" if is_creator else "Total Cumulative Spent"
        
        elements = []
        
        header_data = [
            [
                Paragraph("<b>CAPTURO</b><br/><font size=9.5 color='#7B2FBE'><b>" + statement_title + "</b></font>", ParagraphStyle('Logo', parent=body_style, fontSize=18, leading=22)),
                Paragraph("<b>ACCOUNT STATEMENT</b><br/><font size=9.5>Issued to: " + (user.full_name or "User") + "</font>", ParagraphStyle('HeaderRight', parent=body_style, alignment=2, leading=14))
            ]
        ]
        header_table = Table(header_data, colWidths=[310, 230])
        header_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 12),
        ]))
        elements.append(header_table)
        
        divider = Table([[""]], colWidths=[540])
        divider.setStyle(TableStyle([
            ('LINEBELOW', (0,0), (-1,-1), 2, accent_pink),
            ('BOTTOMPADDING', (0,0), (-1,-1), 0),
            ('TOPPADDING', (0,0), (-1,-1), 0),
        ]))
        elements.append(divider)
        elements.append(Spacer(1, 15))
        
        # Summary details
        total_spent = sum(float(b.total_amount) for b in bookings)
        summary_intro = (
            f"<b>Statement Summary:</b><br/>"
            f"Account Holder: <b>{user.full_name or 'User'}</b><br/>"
            f"Email Address: <b>{user.email or 'N/A'}</b><br/>"
            f"Total Completed Sessions: <b>{len(bookings)}</b><br/>"
            f"{total_label}: <b>INR {total_spent:,.2f}</b>"
        )
        summary_table = Table([[Paragraph(summary_intro, body_style)]], colWidths=[540])
        summary_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), bg_light),
            ('TOPPADDING', (0,0), (-1,-1), 10),
            ('BOTTOMPADDING', (0,0), (-1,-1), 10),
            ('LEFTPADDING', (0,0), (-1,-1), 12),
            ('RIGHTPADDING', (0,0), (-1,-1), 12),
            ('LINELEFT', (0,0), (-1,-1), 3, accent_color),
        ]))
        elements.append(summary_table)
        elements.append(Spacer(1, 20))
        
        # Bookings transactions list table
        table_data = [
            [
                Paragraph("<b>Date</b>", bold_body_style),
                Paragraph("<b>Service Description</b>", bold_body_style),
                Paragraph("<b>" + ("Attendee / Paid By" if is_creator else "Creator / Paid To") + "</b>", bold_body_style),
                Paragraph("<b>Status</b>", bold_body_style),
                Paragraph("<b>Amount</b>", bold_body_style)
            ]
        ]
        
        for b in bookings:
            if is_creator:
                party_name = b.attendee.full_name if b.attendee else "Client Partner"
                amount_str = f"+ INR {float(b.total_amount):,.2f}"
            else:
                party_name = b.creator.full_name if b.creator else "Creator Partner"
                amount_str = f"- INR {float(b.total_amount):,.2f}"
            table_data.append([
                Paragraph(str(b.event_date), body_style),
                Paragraph(f"<b>{b.event_type.title()} Session</b><br/><font size=8 color='#5E5E5E'>Loc: {b.location}</font>", body_style),
                Paragraph(party_name, body_style),
                Paragraph(b.status.upper(), bold_body_style),
                Paragraph(amount_str, bold_body_style)
            ])
            
        items_table = Table(table_data, colWidths=[80, 170, 120, 80, 90])
        items_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('BACKGROUND', (0,0), (-1,0), bg_light),
            ('LINEBELOW', (0,0), (-1,0), 1, accent_color),
            ('LINEBELOW', (0,1), (-1,-1), 0.5, colors.HexColor("#E0E0E0")),
            ('TOPPADDING', (0,0), (-1,-1), 10),
            ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ]))
        elements.append(items_table)
        elements.append(Spacer(1, 30))
        
        footer_style = ParagraphStyle(
            'Footer', parent=body_style, fontName='Helvetica-Oblique', fontSize=8.5, textColor=colors.HexColor("#888888"), alignment=1
        )
        elements.append(Paragraph("This is an AI-generated statement of payments for your Capturo account.", footer_style))
        elements.append(Paragraph("For support or queries, contact support@capturo.com | www.capturo.com", footer_style))
        
        doc.build(elements)

invoice_service = InvoiceService()

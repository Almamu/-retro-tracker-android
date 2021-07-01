package com.glintdg.retrotrackerandroid.ui.main

import android.R.attr
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.glintdg.retrotrackerandroid.databinding.FragmentMainBinding
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject
import kotlin.properties.Delegates


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var queue: RequestQueue
    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    var mode: Int = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }

        this.queue = Volley.newRequestQueue (requireContext ())
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val root = binding.root

        val videogamesButton = binding.videogameButton;
        videogamesButton.setOnClickListener { _ ->
            this.mode = 1;
            IntentIntegrator.forSupportFragment (this).initiateScan(); // `this` is the current Activity
        }

        val booksButton = binding.bookButton;
        booksButton.setOnClickListener { _ ->
            this.mode = 2;
            IntentIntegrator.forSupportFragment (this).initiateScan(); // `this` is the current Activity
        }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this.context, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                var url = when (this.mode)
                {
                    1 -> "http://10.0.2.1:8000/isbn/" + result.contents
                    else -> "http://10.0.2.1:8000/ean/" + result.contents
                }

                val request = object : JsonObjectRequest (
                    Request.Method.GET,
                    url, null,
                    Response.Listener <JSONObject> { response ->
                        var name = when (this.mode)
                        {
                            1 -> response.getString ("title")
                            else -> response.getString ("description")
                        }

                        Toast.makeText (this.context, "Found: " + name, Toast.LENGTH_LONG).show ()

                        val json = JSONObject ()

                        json.put ("name", name)
                        json.put ("barcode", result.contents)

                        val request = object: JsonObjectRequest (
                            Request.Method.POST,
                            "http://10.0.2.1:8000/collections/" + this.mode + "/items",
                            json,
                            Response.Listener <JSONObject> {
                               Toast.makeText (this.context, name + " uploaded correctly", Toast.LENGTH_LONG).show ()
                            },
                            Response.ErrorListener {
                                Toast.makeText (this.context, name + " failed upload", Toast.LENGTH_LONG).show ()
                            }) {}

                        this.queue.add (request)
                    },
                    Response.ErrorListener {
                        Toast.makeText (this.context, "Cannot find product in the databases", Toast.LENGTH_LONG).show ()
                    }) {}

                this.queue.add (request);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}